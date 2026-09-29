package net.diexv.potionenchant.SkyRender.client.model;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mojang.math.Transformation;
import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.SkyRender.api.client.model.PerspectiveModelState;
import net.diexv.potionenchant.SkyRender.client.shader.AvaritiaShaders;
import net.diexv.potionenchant.SkyRender.client.shader.DiexvSwordShaders;
import net.diexv.potionenchant.SkyRender.util.client.TransformUtils;
import net.diexv.potionenchant.client.compat.oculus.CosmicItemLateRenderQueue;
import net.diexv.potionenchant.client.compat.oculus.ItemShaderModCompat;
import net.diexv.potionenchant.client.compat.oculus.WorldRenderPhase;
import net.diexv.potionenchant.client.renderer.CutterAttackAnimation;
import net.diexv.potionenchant.client.renderer.SwordAuraRenderer;
import net.diexv.potionenchant.client.renderer.coderain.CodeRainRenderer;
import net.diexv.potionenchant.client.renderer.gl.SnowflakeRenderer;
import net.diexv.potionenchant.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.*;

public final class CosmicBakeModel implements BakedModel {
    private static final ItemModelGenerator ITEM_MODEL_GENERATOR = new ItemModelGenerator();
    private static final FaceBakery FACE_BAKERY = new FaceBakery();
    private static final int COSMIC_TEXTURE_COUNT = 25;
    private final List<ResourceLocation> maskSprite;
    private final BakedModel wrapped;
    private final ItemOverrides overrideList;
    private ModelState parentState;
    private LivingEntity entity;
    private ClientLevel world;

    public static boolean isBlockContext(ItemDisplayContext context) {
        return switch (context) {
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND,
                 FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND,
                 GROUND, FIXED, GUI -> true;
            default -> false;
        };
    }

    /** 剑/code 物品：3D 环绕代码雨粒子 */
    public static boolean isCodeRainItem(ItemStack stack) {
        return stack != null && (stack.getItem() == ModItems.CODE.get() || stack.getItem() == ModItems.DIEXV_SWORD.get());
    }

    public CosmicBakeModel(final BakedModel wrapped, final List<ResourceLocation> maskSprite) {
        this.overrideList = new ItemOverrides() {
            @Override
            public BakedModel resolve(final @NotNull BakedModel originalModel, final @NotNull ItemStack stack, final ClientLevel world, final LivingEntity entity, final int seed) {
                CosmicBakeModel.this.entity = entity;
                CosmicBakeModel.this.world = (world != null) ? world : (entity == null ? null : (ClientLevel) entity.level());
                return CosmicBakeModel.this.wrapped.getOverrides().resolve(originalModel, stack, world, entity, seed);
            }
        };
        this.wrapped = wrapped;
        this.parentState = TransformUtils.stateFromItemTransforms(wrapped.getTransforms());
        this.maskSprite = maskSprite;
    }

    public void renderItem(ItemStack stack, ItemDisplayContext transformType, PoseStack pStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        RenderType renderType = AvaritiaShaders.COSMIC_RENDER_TYPE;

        if (stack.getItem() == ModItems.UNIVERSAL_POTION_BOTTLE.get() || stack.getItem() == ModItems.DIEXV_SWORD.get()) {
            this.parentState = TransformUtils.DEFAULT_TOOL;
        } else {
            this.parentState = TransformUtils.stateFromItemTransforms(wrapped.getTransforms());
        }

        // 先渲染基底模型
        BakedModel model = this.wrapped.getOverrides().resolve(this.wrapped, stack, this.world, this.entity, 0);
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        assert model != null;

        // DiexvSword（diexv_sword）：常态即用自实现体素多面体替代原贴图（剑形，自识别贴图构建）。
        boolean voxelReplace = stack.getItem() == ModItems.DIEXV_SWORD.get()
                && DiexvSwordVoxelMesh.shouldVoxelReplace(transformType);
        if (voxelReplace) {
            DiexvSwordVoxelMesh.renderMesh(pStack, buffers);
            if (buffers instanceof MultiBufferSource.BufferSource source) {
                source.endBatch();
            }
        } else {
            Set<RenderType> baseRenderTypes = new LinkedHashSet<>();
            for (BakedModel bakedModel : model.getRenderPasses(stack, true)) {
                for (RenderType rendertype : bakedModel.getRenderTypes(stack, true)) {
                    itemRenderer.renderModelLists(bakedModel, stack, packedLight, packedOverlay, pStack,
                            buffers.getBuffer(rendertype));
                    baseRenderTypes.add(rendertype);
                }
            }
            if (buffers instanceof MultiBufferSource.BufferSource source) {
                baseRenderTypes.forEach(source::endBatch);
            }
        }

        // 月牙剑气：只在【第一人称手持】渲染里绘制。
        // 绝不能包含 GUI（物品栏/快捷栏）——否则挥一把剑时，物品栏里那把也会跟着放剑气；
        // 第三人称手持也不在这里画（那条由 SwordAuraThirdPerson 的事件单独负责）。
        if (CutterAttackAnimation.isSwinging()
                && (transformType == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                    || transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND)) {
            SwordAuraRenderer.renderFirstPerson(pStack, buffers);
        }

        // 光影兼容：延迟渲染（包含手持渲染用的 GUI 上下文）。
        // 【必须限定在 GameRenderer.renderLevel 期间】原因见 WorldRenderPhase 注释：
        // 快捷栏/HUD 的 GUI 物品是在 renderLevel 之后、主渲染目标上画的，不需要也无法延迟
        // （延迟到世界 pass 反而会被随后绘制的 HUD 盖掉 → 表现为"快捷栏里没有着色器效果"）。
        boolean shouldDefer = !AvaritiaShaders.inventoryRender
            && WorldRenderPhase.isInWorldRender()
            && supportsLateRenderType(renderType)
            && (ItemShaderModCompat.shouldDeferItemShaderLayer(transformType)
                || (ItemShaderModCompat.shouldDeferCosmicItemRendering() && transformType == ItemDisplayContext.GUI));
        if (shouldDefer) {
            // 剑/code 的 mask 流光用源项目 cosmic（代码雨），其余用本模组 cosmic
            boolean codeRainMask = isCodeRainItem(stack);
            if (codeRainMask ? !DiexvSwordShaders.isSwordCosmicReady() : !isShaderLayerReady(renderType)) {
                return;
            }
            CosmicItemLateRenderQueue.enqueue(this, stack, transformType, pStack, packedLight, packedOverlay, model, renderType);
            return;
        }

        // 粒子特效：剑/code → 3D 环绕代码雨；其余 → 雪花飘落粒子。
        // 注意：Oculus 光影下 renderItem 会在上面的 shouldDefer 分支直接 return，
        // 粒子改由 CosmicItemLateRenderQueue 重画 —— 所以粒子必须走这个公共方法，
        // 否则挥砍期间的"不跟随模型"补偿在光影下不会生效。
        renderItemParticles(stack, transformType, pStack, buffers, packedLight, packedOverlay);

        // 正常渲染星空层
        renderShaderLayer(stack, transformType, pStack, buffers, packedLight, packedOverlay, model, renderType, false);
    }

    public void renderShaderLayer(ItemStack stack, ItemDisplayContext transformType, PoseStack pStack, MultiBufferSource buffers, int packedLight, int packedOverlay, BakedModel model, RenderType renderType, boolean lateRender) {
        // 剑/code：mask 流光使用源项目 cosmic（天空代码雨）着色器；其他物品：本模组 cosmic
        boolean codeRainMask = isCodeRainItem(stack);
        if (codeRainMask) {
            if (!DiexvSwordShaders.isSwordCosmicReady()) {
                return;
            }
        } else if (!isShaderLayerReady(renderType)) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        if (codeRainMask) {
            // GUI 模式：缩小星体 + 固定视角（upload 内部处理）；时间基准按延迟/非延迟与源项目一致
            boolean gui = AvaritiaShaders.inventoryRender || transformType == ItemDisplayContext.GUI;
            renderType = DiexvSwordShaders.SWORD_COSMIC_RENDER_TYPE;
            DiexvSwordShaders.uploadSwordCosmicUniforms(gui, lateRender);
        } else {
            // 提前上传 uniform（使用 AvaritiaShaders 的统一方法）
            AvaritiaShaders.uploadCosmicUniforms();

            // GUI 模式：缩小星体 + 固定视角
            if (AvaritiaShaders.inventoryRender || transformType == ItemDisplayContext.GUI) {
                if (AvaritiaShaders.cosmicExternalScale != null) {
                    AvaritiaShaders.cosmicExternalScale.set(100.0F);
                }
                if (AvaritiaShaders.cosmicYaw != null) {
                    AvaritiaShaders.cosmicYaw.set(0.0F);
                }
                if (AvaritiaShaders.cosmicPitch != null) {
                    AvaritiaShaders.cosmicPitch.set(0.0F);
                }
            }
        }

        if (lateRender) {
            renderType = lateRenderType(renderType, transformType, codeRainMask);
        }

        try {
            VertexConsumer buffersBuffer = buffers.getBuffer(renderType);

            // 3D 方块型模型
            if (model.isGui3d() && isBlockContext(transformType)) {
                List<BakedQuad> blockLayer = new ArrayList<>();
                RandomSource random = RandomSource.create();
                for (Direction direction : Direction.values()) {
                    blockLayer.addAll(model.getQuads(null, direction, random));
                }

                List<TextureAtlasSprite> maskSprites = new ArrayList<>();
                for (ResourceLocation res : maskSprite) {
                    maskSprites.add(mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(res));
                }

                List<BakedQuad> overlayQuads = new ArrayList<>();
                for (BakedQuad base : blockLayer) {
                    for (TextureAtlasSprite sprite : maskSprites) {
                        overlayQuads.add(textureQuadBestEffort(base, sprite));
                    }
                }
                mc.getItemRenderer().renderQuadList(pStack, buffersBuffer, overlayQuads, stack, packedLight, packedOverlay);
            } else {
                // 平面/精灵层模型（书、药水瓶、剑/code）—— 源项目 bakeMaskQuads
                LinkedList<BakedQuad> quads = new LinkedList<>();
                List<TextureAtlasSprite> atlasSprite = new ArrayList<>();
                for (ResourceLocation res : maskSprite) {
                    atlasSprite.add(mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(res));
                }
                for (int i = 0; i < atlasSprite.size(); i++) {
                    quads.addAll(bakeMaskQuads(atlasSprite.get(i), i));
                }
                mc.getItemRenderer().renderQuadList(pStack, buffersBuffer, quads, stack, packedLight, packedOverlay);
            }
        } finally {
            if (!lateRender && buffers instanceof MultiBufferSource.BufferSource source) {
                source.endBatch(renderType);
            }
        }
    }

    private static boolean supportsLateRenderType(RenderType renderType) {
        return renderType == AvaritiaShaders.COSMIC_RENDER_TYPE
                || renderType == DiexvSwordShaders.SWORD_COSMIC_RENDER_TYPE;
    }

    /** 源项目同款：把 mask 贴图烘焙为单层 quad（processFrames + bakeQuad） */
    private LinkedList<BakedQuad> bakeMaskQuads(TextureAtlasSprite sprite, int layerIndex) {
        LinkedList<BakedQuad> quads = new LinkedList<>();
        List<BlockElement> unbaked = ITEM_MODEL_GENERATOR.processFrames(layerIndex, "layer" + layerIndex, sprite.contents());
        for (BlockElement element : unbaked) {
            for (Map.Entry<Direction, BlockElementFace> entry : element.faces.entrySet()) {
                quads.add(FACE_BAKERY.bakeQuad(element.from, element.to, entry.getValue(), sprite, entry.getKey(),
                        new PerspectiveModelState(ImmutableMap.of()), element.rotation, element.shade,
                        PotionEnchantMod.rl("dynamic")));
            }
        }
        return quads;
    }

    /**
     * 物品 GL 粒子的统一绘制入口（雪花 / 代码雨）。
     *
     * <p>【挥砍期间】粒子不跟着模型运动：粒子仍按自己的时间下落/滚动，只是不再继承挥砍变换 ——
     * 做法是把"挥砍变换"换成"原版持握变换"（只作用于粒子这一段，模型本体照旧挥动）。
     *
     * <p><b>两条渲染路径都必须调用本方法</b>：普通即时渲染（{@code renderItem}）与
     * Oculus 光影下的延迟渲染（{@code CosmicItemLateRenderQueue}）。后者是光影把世界合成到
     * 主渲染目标之后再补画的一遍，之前漏掉这里，导致光影模式下粒子仍然跟着挥动。
     */
    public static void renderItemParticles(ItemStack stack, ItemDisplayContext transformType, PoseStack pStack,
                                           MultiBufferSource buffers, int packedLight, int packedOverlay) {
        boolean firstPersonSwing = CutterAttackAnimation.isSwinging()
                && (transformType == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                    || transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
        if (firstPersonSwing) {
            pStack.pushPose();
            pStack.mulPoseMatrix(particleHoldMatrix(transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND));
        }
        if (isCodeRainItem(stack)) {
            CodeRainRenderer.renderCodeRain(pStack, buffers, packedLight, packedOverlay);
        } else {
            renderSnowflakes(transformType, pStack, buffers, packedLight, packedOverlay);
        }
        if (firstPersonSwing) {
            pStack.popPose();
        }
    }

    /**
     * 挥砍期间给粒子用的补偿矩阵：把 pose 里的"挥砍变换"换成"原版持握变换"，
     * 于是粒子留在原地（不再跟着刀身挥动），而它自己的下落/滚动动画不受影响。
     * <pre>
     *   pose = V · 挥砍 · D · T(-0.5)，目标 = V · 持握 · D · T(-0.5)
     *   补偿 = (D·T(-0.5))⁻¹ · 挥砍⁻¹ · 持握 · D·T(-0.5)      （外层 V 会被抵消，无需知道）
     * </pre>
     */
    public static org.joml.Matrix4f particleHoldMatrix(boolean leftHand) {
        org.joml.Matrix4f dispHalf = SwordAuraRenderer.displayMatrix(leftHand).translate(-0.5F, -0.5F, -0.5F);
        org.joml.Matrix4f swing = SwordAuraRenderer.chainMatrix(CutterAttackAnimation.currentProgress());
        float side = leftHand ? -1.0F : 1.0F;
        // 原版 applyItemArmTransform（equip 视为 0：挥砍时手早已落位）
        org.joml.Matrix4f hold = new org.joml.Matrix4f().translate(side * 0.56F, -0.52F, -0.72F);
        return new org.joml.Matrix4f(dispHalf).invert()
                .mul(new org.joml.Matrix4f(swing).invert())
                .mul(hold)
                .mul(dispHalf);
    }

    private static boolean isShaderLayerReady(RenderType renderType) {        return AvaritiaShaders.cosmicShader != null
            && AvaritiaShaders.cosmicTime != null
            && AvaritiaShaders.cosmicYaw != null
            && AvaritiaShaders.cosmicPitch != null
            && AvaritiaShaders.cosmicExternalScale != null
            && AvaritiaShaders.cosmicOpacity != null
            && AvaritiaShaders.cosmicUVs != null;
    }

    public static void renderSnowflakes(ItemDisplayContext transformType, PoseStack pStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        pStack.pushPose();
        if (transformType != ItemDisplayContext.GUI) {
            org.joml.Matrix4f mat = pStack.last().pose();
            float sx = (float)Math.sqrt(mat.m00() * mat.m00() + mat.m10() * mat.m10() + mat.m20() * mat.m20());
            float sy = (float)Math.sqrt(mat.m01() * mat.m01() + mat.m11() * mat.m11() + mat.m21() * mat.m21());
            float sz = (float)Math.sqrt(mat.m02() * mat.m02() + mat.m12() * mat.m12() + mat.m22() * mat.m22());
            float avgScale = (sx + sy + sz) / 3.0f;

            org.joml.Matrix4f invRot = new org.joml.Matrix4f();
            invRot.m00(mat.m00() / sx); invRot.m01(mat.m10() / sx); invRot.m02(mat.m20() / sx); invRot.m03(0);
            invRot.m10(mat.m01() / sy); invRot.m11(mat.m11() / sy); invRot.m12(mat.m21() / sy); invRot.m13(0);
            invRot.m20(mat.m02() / sz); invRot.m21(mat.m12() / sz); invRot.m22(mat.m22() / sz); invRot.m23(0);
            invRot.m30(0); invRot.m31(0); invRot.m32(0); invRot.m33(1);

            pStack.last().pose().mul(invRot);
            pStack.scale(1.0f / avgScale, 1.0f / avgScale, 1.0f / avgScale);
        } else {
            org.joml.Matrix4f mat = pStack.last().pose();
            float sx = (float)Math.sqrt(mat.m00() * mat.m00() + mat.m10() * mat.m10() + mat.m20() * mat.m20());
            float sy = (float)Math.sqrt(mat.m01() * mat.m01() + mat.m11() * mat.m11() + mat.m21() * mat.m21());
            float sz = (float)Math.sqrt(mat.m02() * mat.m02() + mat.m12() * mat.m12() + mat.m22() * mat.m22());
            float invScale = 1.0f / ((sx + sy + sz) / 3.0f);
            pStack.scale(invScale, invScale, invScale);
        }
        SnowflakeRenderer.renderSnowflakes(pStack, buffers, packedLight, packedOverlay);
        pStack.popPose();
    }

    private static RenderType lateRenderType(RenderType renderType, ItemDisplayContext context, boolean swordCosmic) {
        if (swordCosmic) {
            return isFirstPersonHandContext(context)
                    ? DiexvSwordShaders.SWORD_COSMIC_HAND_AFTER_LEVEL_RENDER_TYPE
                    : DiexvSwordShaders.SWORD_COSMIC_ITEM_AFTER_LEVEL_RENDER_TYPE;
        }
        if (isFirstPersonHandContext(context)) {
            return AvaritiaShaders.COSMIC_HAND_AFTER_LEVEL_RENDER_TYPE;
        }
        return AvaritiaShaders.COSMIC_ITEM_AFTER_LEVEL_RENDER_TYPE;
    }

    private static boolean isFirstPersonHandContext(ItemDisplayContext context) {
        return context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || context == ItemDisplayContext.GUI;
    }

    private static BakedQuad textureQuadBestEffort(BakedQuad base, TextureAtlasSprite sprite) {
        try {
            int[] vert = base.getVertices().clone();
            return new BakedQuad(vert, base.getTintIndex(), base.getDirection(), sprite, base.isShade());
        } catch (Throwable throwable) {
            PotionEnchantMod.LOGGER.warn("CosmicBakeModel: unable to texture quad (falling back to base quad)", throwable);
            return base;
        }
    }

    @Override
    public @NotNull BakedModel applyTransform(@NotNull ItemDisplayContext context, @NotNull PoseStack pStack, boolean leftFlip) {
        PerspectiveModelState modelState = (PerspectiveModelState) this.parentState;
        if (modelState != null) {
            Transformation transform = ((PerspectiveModelState) this.parentState).getTransform(context);
            Vector3f trans = transform.getTranslation();
            Vector3f scale = transform.getScale();
            pStack.translate(trans.x(), trans.y(), trans.z());
            pStack.mulPose(transform.getLeftRotation());
            pStack.scale(scale.x(), scale.y(), scale.z());
            pStack.mulPose(transform.getRightRotation());
            if (leftFlip) {
                pStack.mulPose(Axis.YN.rotationDegrees(180.0f));
            }
            return this;
        }
        return BakedModel.super.applyTransform(context, pStack, leftFlip);
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(BlockState state, Direction side, @NotNull RandomSource rand) {
        // 本模型的视觉效果全部由 renderItem(...) 用代码画（贴图模型只作为形状/变换来源），
        // 所以这里的四边形原本是空的。但"空四边形 + isCustomRenderer()==true"有隐患：
        // 没有声明 getCustomRenderer 接缝的路径（别的物品用到本模型、或第三方渲染器直接走四边形路径）
        // 会退化成【什么都不画】。回落到被包裹模型后，这些路径至少画出原贴图，不会出现空白物品；
        // 本物品的正常路径不受影响（它走 renderItem）。
        if (this.wrapped instanceof CosmicBakeModel) {
            return Collections.emptyList();   // 防自递归（正常路径下 wrapped 是贴图模型，不是本类）
        }
        try {
            return this.wrapped.getQuads(state, side, rand);
        } catch (Throwable ignored) {
            return Collections.emptyList();
        }
    }

    @Override
    public @NotNull TextureAtlasSprite getParticleIcon() {
        return this.wrapped.getParticleIcon();
    }

    @Override
    public @NotNull TextureAtlasSprite getParticleIcon(@NotNull ModelData data) {
        return this.wrapped.getParticleIcon(data);
    }

    @Override
    public @NotNull ItemOverrides getOverrides() {
        return this.overrideList;
    }

    @Override
    public boolean isCustomRenderer() {
        return true;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return this.wrapped.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return this.wrapped.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return this.wrapped.usesBlockLight();
    }
}