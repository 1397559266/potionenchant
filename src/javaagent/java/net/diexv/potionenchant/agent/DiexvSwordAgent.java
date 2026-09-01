package net.diexv.potionenchant.agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

/**
 * DiexvSword JavaAgent（Attach 动态附加）—— 平移自 DiexvSword V6 DiexvAgent 的
 * "百分比伤害 redefine"实现。
 *
 * 由 DiexvSwordTransformationService（coremod SERVICE 阶段）通过 Attach API 动态附加，
 * Launcher 把 agent jar 追加到 bootstrap 类路径，注入代码引用的 helper
 * （DiexvSwordZeroState）对 SynchedEntityData（Platform 加载）可见。
 *
 * 目标方法：SynchedEntityData#set(EntityDataAccessor, Object)
 *  - 开发环境（official 映射）：方法名 set
 *  - 生产环境（SRG 映射）：方法名 m_135381_
 *  - 描述符在两种映射下完全相同，类名均为 official，匹配逻辑同时覆盖两种情况。
 *
 * 行为（与 V6 等价，语义=目标归零）：
 *  if (DiexvSwordZeroState.shouldApplyPercentageDamage(this) && value instanceof Float) {
 *      float damage = DiexvSwordZeroState.getEntityDamageAmount(this);   // Float.MAX_VALUE
 *      if (damage > 0 && (Float) value > 0) {
 *          value = Float.valueOf(Math.max((Float) value - damage, 0.0f));  // -> 0
 *          DiexvSwordZeroState.consumePercentageDamage(this);
 *      }
 *  }
 *
 * redefine（由 DiexvSword 标记目标触发）：
 *  - 加载期：注册转换器（canRetransform=true），SynchedEntityData 首次加载即注入。
 *  - 运行时（ensureRedefined，剑标记触发）：对已加载的 SynchedEntityData 执行
 *    retransformOrRedefine——SynchedEntityData 已被 mixin 加接口，用 jar 原始字节
 *    redefineClasses 会被 JVM 拒绝，走 retransformClasses（重跑转换器链，幂等防重复）。
 */
public final class DiexvSwordAgent {

    private static final String TAG = "[DiexvSwordAgent]";

    private static final String TARGET_CLASS_INTERNAL = "net/minecraft/network/syncher/SynchedEntityData";

    // 注入代码引用的 agent 自包含 helper 类（bootstrap 可见）
    private static final String UTIL = "net/diexv/potionenchant/agent/DiexvSwordZeroState";

    // 目标方法描述符：official / SRG 下完全一致
    private static final String TARGET_METHOD_DESC =
            "(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;)V";

    private DiexvSwordAgent() {
    }

    // ==================== 入口 ====================

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println(TAG + " premain 启动 (args=" + agentArgs + ")");
        install(inst);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println(TAG + " agentmain 启动 (attach 模式, args=" + agentArgs + ")");
        install(inst);
    }

    public static volatile Instrumentation INST;
    private static volatile boolean synchedPatched = false;

    public static void install(Instrumentation inst) {
        INST = inst;
        // 加载期转换器（canRetransform=true：加载期 + retransform 双通道）
        inst.addTransformer(new java.lang.instrument.ClassFileTransformer() {
            @Override
            public byte[] transform(java.lang.Module module, ClassLoader loader, String className,
                                    Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
                                    byte[] classfileBuffer) {
                byte[] out = DiexvSwordAgent.transform(module, loader, className, classBeingRedefined,
                        protectionDomain, classfileBuffer);
                if (out != null) {
                    synchedPatched = true;
                }
                return out;
            }
        }, true);
        System.out.println(TAG + " install 完成（转换器已注册；redefine 由剑标记目标触发）");
    }

    // ==================== DiexvSword 触发的 redefine（V6 sweepLoadedClasses + retransformOrRedefine） ====================

    /** 由剑标记目标时调用：扫描已加载目标类，未注入的走 retransformOrRedefine */
    public static void ensureRedefined() {
        Instrumentation inst = INST;
        if (inst == null) {
            System.out.println(TAG + " ensureRedefined: INST 为空（agent 未附加）");
            return;
        }
        for (Class<?> c : inst.getAllLoadedClasses()) {
            if (c == null || !inst.isModifiableClass(c)) {
                continue;
            }
            if (isTargetClassName(c.getName()) && !synchedPatched) {
                retransformOrRedefine(inst, c);
            }
        }
    }

    /**
     * V6 retransformOrRedefine：SynchedEntityData 已被 mixin 添加接口，用 jar 原始字节
     * redefineClasses 会报 "attempted to change superclass or interfaces"；
     * retransform 基于当前已插桩字节重跑转换器链（含本 agent 转换器，幂等防重复注入）。
     */
    private static void retransformOrRedefine(Instrumentation inst, Class<?> clazz) {
        try {
            inst.retransformClasses(clazz);
            synchedPatched = true;
            System.out.println(TAG + " 已 retransform " + clazz.getName() + " (DiexvSword 触发)");
        } catch (Throwable t) {
            System.err.println(TAG + " retransform " + clazz.getName() + " 失败: " + t);
            t.printStackTrace();
        }
    }

    private static byte[] readRawClassBytes(Class<?> clazz) {
        String resource = clazz.getName().replace('.', '/') + ".class";
        ClassLoader loader = clazz.getClassLoader();
        try (InputStream in = loader != null
                ? loader.getResourceAsStream(resource)
                : ClassLoader.getSystemResourceAsStream(resource)) {
            if (in == null) {
                return null;
            }
            return in.readAllBytes();
        } catch (IOException e) {
            return null;
        }
    }

    // ==================== 转换器 ====================

    private static byte[] transform(java.lang.Module module, ClassLoader loader, String className,
                                    Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
                                    byte[] classfileBuffer) {
        if (className == null || !className.replace('.', '/').equals(TARGET_CLASS_INTERNAL)) {
            return null;
        }
        try {
            byte[] patched = patch(classfileBuffer, loader);
            if (patched != null) {
                System.out.println(TAG + " 已通过类加载转换器改写 " + className);
            }
            return patched;
        } catch (Throwable t) {
            System.err.println(TAG + " 改写 " + className + " 失败: " + t);
            t.printStackTrace();
            return null;
        }
    }

    private static boolean isTargetClassName(String className) {
        if (className == null) {
            return false;
        }
        String internal = className.replace('.', '/');
        return internal.equals(TARGET_CLASS_INTERNAL)
                || internal.endsWith("/network/syncher/SynchedEntityData");
    }

    // ==================== ASM 改写（V6 patch + SetMethodPatcher） ====================

    private static byte[] patch(byte[] original, ClassLoader loader) {
        // 幂等：目标方法已含 shouldApplyPercentageDamage 调用（之前已注入过）则跳过，
        // 避免 retransform/强制重补时重复注入导致归零逻辑跑两次
        if (isSynchedSetPatched(new ClassReader(original))) {
            return null;
        }
        ClassReader reader = new ClassReader(original);

        // 第一遍：找到目标方法，记录原始 maxLocals，用于挑选空闲局部变量槽
        int originalMaxLocals = findTargetMethodMaxLocals(reader);
        if (originalMaxLocals < 0) {
            System.out.println(TAG + " 未找到目标方法，跳过: " + TARGET_METHOD_DESC);
            return null;
        }

        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
            @Override
            protected String getCommonSuperClass(String type1, String type2) {
                // 用目标类所在 classloader 解析类型，避免 Minecraft 类无法加载导致帧计算失败
                ClassLoader cl = loader != null ? loader : DiexvSwordAgent.class.getClassLoader();
                try {
                    Class<?> c1 = Class.forName(type1.replace('/', '.'), false, cl);
                    Class<?> c2 = Class.forName(type2.replace('/', '.'), false, cl);
                    if (c1.isAssignableFrom(c2)) {
                        return type1;
                    }
                    if (c2.isAssignableFrom(c1)) {
                        return type2;
                    }
                    if (c1.isInterface() || c2.isInterface()) {
                        return "java/lang/Object";
                    }
                    do {
                        c1 = c1.getSuperclass();
                    } while (c1 != null && !c1.isAssignableFrom(c2));
                    return c1 != null ? c1.getName().replace('.', '/') : "java/lang/Object";
                } catch (ClassNotFoundException e) {
                    return "java/lang/Object";
                }
            }
        };

        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (TARGET_METHOD_DESC.equals(descriptor) && isTargetMethodName(name)) {
                    return new SetMethodPatcher(delegate, originalMaxLocals);
                }
                return delegate;
            }
        };
        reader.accept(visitor, ClassReader.SKIP_FRAMES);
        return writer.toByteArray();
    }

    private static boolean isTargetMethodName(String name) {
        // official: set ；SRG: m_135381_ ；兼容 func_ 前缀兜底
        return name.equals("set") || name.startsWith("m_") || name.startsWith("func_");
    }

    /** 目标方法体是否已含 DiexvSwordZeroState.shouldApplyPercentageDamage 调用（幂等检测） */
    private static boolean isSynchedSetPatched(ClassReader reader) {
        final boolean[] found = {false};
        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                if (TARGET_METHOD_DESC.equals(descriptor) && isTargetMethodName(name)) {
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override
                        public void visitMethodInsn(int opcode, String owner, String mname,
                                                    String mdesc, boolean itf) {
                            if (opcode == Opcodes.INVOKESTATIC && owner.equals(UTIL)
                                    && mname.equals("shouldApplyPercentageDamage")) {
                                found[0] = true;
                            }
                        }
                    };
                }
                return null;
            }
        }, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
        return found[0];
    }

    private static int findTargetMethodMaxLocals(ClassReader reader) {
        int[] holder = {-1};
        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                if (TARGET_METHOD_DESC.equals(descriptor) && isTargetMethodName(name)) {
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override
                        public void visitMaxs(int maxStack, int maxLocals) {
                            holder[0] = maxLocals;
                        }
                    };
                }
                return null;
            }
        }, ClassReader.SKIP_FRAMES);
        return holder[0];
    }

    /**
     * 在目标方法开头插入"目标归零"逻辑（直接归 0，无需伤害计算）：
     *  if (shouldApplyPercentageDamage(this) && value instanceof Float) {
     *      value = Float.valueOf(0.0f);   // 目标血量数据直接归零
     *  }
     */
    private static final class SetMethodPatcher extends MethodVisitor {

        private final Label originalCode = new Label();

        SetMethodPatcher(MethodVisitor delegate, int originalMaxLocals) {
            super(Opcodes.ASM9, delegate);
        }

        @Override
        public void visitCode() {
            super.visitCode();
            emitInterception();
            super.visitLabel(originalCode);
        }

        private void emitInterception() {
            MethodVisitor mv = this;

            // if (!DiexvSwordZeroState.shouldApplyPercentageDamage(this)) -> 原逻辑
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, UTIL, "shouldApplyPercentageDamage",
                    "(Ljava/lang/Object;)Z", false);
            mv.visitJumpInsn(Opcodes.IFEQ, originalCode);

            // if (!(value instanceof Float)) -> 原逻辑
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitTypeInsn(Opcodes.INSTANCEOF, "java/lang/Float");
            mv.visitJumpInsn(Opcodes.IFEQ, originalCode);

            // value = Float.valueOf(0.0f);（目标实体血量数据直接归 0）
            mv.visitInsn(Opcodes.FCONST_0);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf",
                    "(F)Ljava/lang/Float;", false);
            mv.visitVarInsn(Opcodes.ASTORE, 2);
        }
    }
}