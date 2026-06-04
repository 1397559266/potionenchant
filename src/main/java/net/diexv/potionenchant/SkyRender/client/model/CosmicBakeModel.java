package net.diexv.potionenchant.SkyRender.client.model;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mojang.math.Transformation;
import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.SkyRender.api.client.model.PerspectiveModelState;
import net.diexv.potionenchant.SkyRender.client.shader.AvaritiaShaders;
import net.diexv.potionenchant.SkyRender.util.client.TransformUtils;
import net.diexv.potionenchant.client.compat.oculus.CosmicItemLateRenderQueue;
import net.diexv.potionenchant.client.compat.oculus.ItemShaderModCompat;
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

        if (stack.getItem() == ModItems.UNIVERSAL_POTION_BOTTLE.get()) {
            this.parentState = TransformUtils.DEFAULT_TOOL;
        } else {
            this.parentState = TransformUtils.stateFromItemTransforms(wrapped.getTransforms());
        }

        // 先渲染基底模型
        BakedModel model = this.wrapped.getOverrides().resolve(this.wrapped, stack, this.world, this.entity, 0);
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        assert model != null;
        Set<RenderType> baseRenderTypes = new LinkedHashSet<>();
        for (BakedModel bakedModel : model.getRenderPasses(stack, true)) {
            for (RenderType rendertype : bakedModel.getRenderTypes(stack, true)) {
                itemRenderer.renderModelLists(bakedModel, stack, packedLight, packedOverlay, pStack, buffers.getBuffer(rendertype));
                baseRenderTypes.add(rendertype);
            }
        }
        if (buffers instanceof MultiBufferSource.BufferSource source) {
            baseRenderTypes.forEach(source::endBatch);
        }

        // 光影兼容：延迟渲染（包含手持渲染用的GUI上下文）
        boolean shouldDefer = !AvaritiaShaders.inventoryRender
            && supportsLateRenderType(renderType)
            && (ItemShaderModCompat.shouldDeferItemShaderLayer(transformType)
                || (ItemShaderModCompat.shouldDeferCosmicItemRendering() && transformType == ItemDisplayContext.GUI));
        if (shouldDefer) {
            if (!isShaderLayerReady(renderType)) {
                return;
            }
            CosmicItemLateRenderQueue.enqueue(this, stack, transformType, pStack, packedLight, packedOverlay, model, renderType);
            return;
        }

        // 正常渲染星空层
        renderShaderLayer(stack, transformType, pStack, buffers, packedLight, packedOverlay, model, renderType, false);
    }

    public void renderShaderLayer(ItemStack stack, ItemDisplayContext transformType, PoseStack pStack, MultiBufferSource buffers, int packedLight, int packedOverlay, BakedModel model, RenderType renderType, boolean lateRender) {
        if (!isShaderLayerReady(renderType)) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        boolean isGUIMode = AvaritiaShaders.inventoryRender || transformType == ItemDisplayContext.GUI;

        // 提前上传 uniform（使用 AvaritiaShaders 的统一方法）
        AvaritiaShaders.uploadCosmicUniforms();

        if (lateRender) {
            renderType = lateRenderType(renderType, transformType);
        }

        // GUI 模式下放大 scale 让星星更明显
        if (isGUIMode) {
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
                // 平面/精灵层模型（书、药水瓶）
                List<TextureAtlasSprite> atlasSprite = new ArrayList<>();
                for (ResourceLocation res : maskSprite) {
                    atlasSprite.add(mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(res));
                }

                LinkedList<BakedQuad> quads = new LinkedList<>();
                for (int i = 0; i < atlasSprite.size(); i++) {
                    TextureAtlasSprite sprite = atlasSprite.get(i);
                    List<BlockElement> unbaked = ITEM_MODEL_GENERATOR.processFrames(i, "layer" + i, sprite.contents());
                    for (BlockElement element : unbaked) {
                        for (Map.Entry<Direction, BlockElementFace> entry : element.faces.entrySet()) {
                            quads.add(FACE_BAKERY.bakeQuad(
                                    element.from, element.to, entry.getValue(),
                                    sprite, entry.getKey(),
                                    new PerspectiveModelState(ImmutableMap.of()),
                                    element.rotation, element.shade,
                                    PotionEnchantMod.rl("dynamic")));
                        }
                    }
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
        return renderType == AvaritiaShaders.COSMIC_RENDER_TYPE;
    }

    private static boolean isShaderLayerReady(RenderType renderType) {
        return AvaritiaShaders.cosmicShader != null
            && AvaritiaShaders.cosmicTime != null
            && AvaritiaShaders.cosmicYaw != null
            && AvaritiaShaders.cosmicPitch != null
            && AvaritiaShaders.cosmicExternalScale != null
            && AvaritiaShaders.cosmicOpacity != null
            && AvaritiaShaders.cosmicUVs != null;
    }

    private static RenderType lateRenderType(RenderType renderType, ItemDisplayContext context) {
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
        return Collections.emptyList();
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
