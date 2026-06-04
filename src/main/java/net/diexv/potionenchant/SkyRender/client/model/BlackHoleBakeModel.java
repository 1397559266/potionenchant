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

public final class BlackHoleBakeModel implements BakedModel {
    private static final ItemModelGenerator ITEM_MODEL_GENERATOR = new ItemModelGenerator();
    private static final FaceBakery FACE_BAKERY = new FaceBakery();
    private final List<ResourceLocation> maskSprite;
    private final BakedModel wrapped;
    private final ItemOverrides overrideList;
    private ModelState parentState;
    private LivingEntity entity;
    private ClientLevel world;

    public BlackHoleBakeModel(final BakedModel wrapped, final List<ResourceLocation> maskSprite) {
        this.overrideList = new ItemOverrides() {
            @Override
            public BakedModel resolve(final @NotNull BakedModel originalModel, final @NotNull ItemStack stack, final ClientLevel world, final LivingEntity entity, final int seed) {
                BlackHoleBakeModel.this.entity = entity;
                BlackHoleBakeModel.this.world = ((world == null) ? ((entity == null) ? null : ((ClientLevel) entity.level())) : null);
                return BlackHoleBakeModel.this.wrapped.getOverrides().resolve(originalModel, stack, world, entity, seed);
            }
        };
        this.wrapped = wrapped;
        this.parentState = TransformUtils.stateFromItemTransforms(wrapped.getTransforms());
        this.maskSprite = maskSprite;
    }

    public void renderItem(ItemStack stack, ItemDisplayContext transformType, PoseStack pStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        if (stack.getItem() == ModItems.UNIVERSAL_POTION_BOTTLE.get()) {
            this.parentState = TransformUtils.DEFAULT_TOOL;
        }
        BakedModel model = this.wrapped.getOverrides().resolve(this.wrapped, stack, this.world, this.entity, 0);
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        assert model != null;
        for (BakedModel bakedModel : model.getRenderPasses(stack, true)) {
            for (RenderType rendertype : bakedModel.getRenderTypes(stack, true)) {
                itemRenderer.renderModelLists(bakedModel, stack, packedLight, packedOverlay, pStack, buffers.getBuffer(rendertype));
            }
        }
        if (buffers instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch();
        }
        Minecraft mc = Minecraft.getInstance();
        float yaw = 0.0F;
        float pitch = 0.0F;
        float scale = 1.0F;  // iZoom: 值越大黑洞越远
        
        // 特殊处理：万能药水附魔瓶即使在GUI模式下也需要跟随视角
        boolean isUniversalBottle = stack.getItem() == ModItems.UNIVERSAL_POTION_BOTTLE.get();
        
        if ((AvaritiaShaders.inventoryRender || transformType == ItemDisplayContext.GUI) && !isUniversalBottle) {
            // GUI/物品栏模式下，不使用视角跟随，使用固定缩放
            scale = 1.0F;
        } else {
            // 手持/第一人称模式下，跟随玩家视角
            assert mc.player != null;
            yaw = (float) (mc.player.getYRot() * 2.0F * Math.PI / 360.0);
            pitch = -(float) (mc.player.getXRot() * 2.0F * Math.PI / 360.0);
        }
        
        // Set black_hole shader uniforms
        AvaritiaShaders.blackHoleTime.set((System.currentTimeMillis() - AvaritiaShaders.renderTime) / 2000.0F);
        AvaritiaShaders.blackHoleYaw.set(yaw);
        AvaritiaShaders.blackHolePitch.set(pitch);
        // iZoom not used - using fixed scale in shader
        
        // Set screen size
        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();
        AvaritiaShaders.blackHoleScreenSize.set(new float[]{width, height});
        
        VertexConsumer cons = buffers.getBuffer(AvaritiaShaders.BLACK_HOLE_RENDER_TYPE);
        List<TextureAtlasSprite> atlasSprite = new ArrayList<>();
        for (ResourceLocation res : maskSprite) {
            atlasSprite.add(Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(res));
        }
        LinkedList<BakedQuad> quads = new LinkedList<>();
        for (TextureAtlasSprite sprite : atlasSprite) {
            List<BlockElement> unbaked = ITEM_MODEL_GENERATOR.processFrames(atlasSprite.indexOf(sprite), "layer" + atlasSprite.indexOf(sprite), sprite.contents());
            for (BlockElement element : unbaked) {
                for (Map.Entry<Direction, BlockElementFace> entry : element.faces.entrySet()) {
                    quads.add(FACE_BAKERY.bakeQuad(element.from, element.to, entry.getValue(), sprite, entry.getKey(), new PerspectiveModelState(ImmutableMap.of()), element.rotation, element.shade, PotionEnchantMod.rl("dynamic")));
                }
            }
        }

        mc.getItemRenderer().renderQuadList(pStack, cons, quads, stack, packedLight, packedOverlay);
    }

    @Override
    public boolean isCustomRenderer() {
        return true;
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
