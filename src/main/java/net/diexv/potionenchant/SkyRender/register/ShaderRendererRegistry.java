package net.diexv.potionenchant.SkyRender.register;

import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.SkyRender.api.shader.ShaderLayerBlock;
import net.diexv.potionenchant.SkyRender.api.shader.ShaderLayerItem;
import net.diexv.potionenchant.SkyRender.api.shader.ShaderLayerModelTransform;
import net.diexv.potionenchant.SkyRender.api.shader.ShaderLayerProperties;
import net.diexv.potionenchant.SkyRender.api.shader.ShaderLayerType;
import net.diexv.potionenchant.SkyRender.client.CosmicRenderProperties;
import net.diexv.potionenchant.SkyRender.client.shader.AvaritiaShaders;
import net.diexv.potionenchant.SkyRender.util.client.TransformUtils;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ShaderRendererRegistry {
    private static final Map<Item, CosmicRenderProperties> RENDER_ITEMS = new ConcurrentHashMap<>();
    private static final Map<Block, CosmicRenderProperties> RENDER_BLOCKS = new ConcurrentHashMap<>();

    public static void registerRenderItem(Item item, CosmicRenderProperties properties) {
        RENDER_ITEMS.put(item, properties);
    }

    public static void registerAll(Collection<Item> items, CosmicRenderProperties properties) {
        for (Item item : items) RENDER_ITEMS.put(item, properties);
    }

    public static void registerAll(Map<Item, CosmicRenderProperties> itemsWithStates) {
        RENDER_ITEMS.putAll(itemsWithStates);
    }

    public static void registerRenderBlock(Block block, CosmicRenderProperties properties) {
        RENDER_BLOCKS.put(block, properties);
    }

    public static void registerAllBlocks(Collection<Block> blocks, CosmicRenderProperties properties) {
        for (Block block : blocks) RENDER_BLOCKS.put(block, properties);
    }

    public static void registerAllBlocks(Map<Block, CosmicRenderProperties> blocksWithStates) {
        RENDER_BLOCKS.putAll(blocksWithStates);
    }

    public static CosmicRenderProperties getPropertiesForStack(ItemStack stack) {
        if (!stack.isEmpty() && stack.getItem() instanceof ShaderLayerItem sli) {
            CosmicRenderProperties p = resolve(sli.getShaderLayer(stack));
            if (p != null) return p;
        }
        return getPropertiesForItem(stack.getItem());
    }

    public static CosmicRenderProperties getPropertiesForItem(Item item) {
        if (item instanceof ShaderLayerItem sli) {
            CosmicRenderProperties p = resolve(sli.getShaderLayer(new ItemStack(item)));
            if (p != null) return p;
        }
        CosmicRenderProperties p = RENDER_ITEMS.get(item);
        if (p != null) return p;
        if (item instanceof BlockItem bi) {
            p = getPropertiesForBlock(bi.getBlock());
            if (p != null) return p;
        }
        return null;
    }

    public static CosmicRenderProperties getPropertiesForBlock(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof ShaderLayerBlock slb) {
            CosmicRenderProperties p = resolve(slb.getShaderLayer(state));
            if (p != null) return p;
        }
        return getPropertiesForBlock(block);
    }

    public static CosmicRenderProperties getPropertiesForBlock(Block block) {
        CosmicRenderProperties p = RENDER_BLOCKS.get(block);
        if (p != null) return p;
        Item item = block.asItem();
        if (item instanceof ShaderLayerItem sli) {
            return resolve(sli.getShaderLayer(new ItemStack(item)));
        }
        return null;
    }

    public static boolean hasCosmicLayer(BlockState state) {
        CosmicRenderProperties p = getPropertiesForBlock(state);
        return p != null && isCosmicRenderType(p.renderType());
    }

    private static CosmicRenderProperties resolve(ShaderLayerProperties props) {
        if (props == null) return null;
        return new CosmicRenderProperties(
            switch (props.modelTransform()) {
                case DEFAULT_TOOL -> TransformUtils.DEFAULT_TOOL;
                default -> TransformUtils.IDENTITY;
            },
            switch (props.layerType()) {
                case COSMIC -> AvaritiaShaders.COSMIC_RENDER_TYPE;
                case SKY_ITEM -> AvaritiaShaders.COSMIC_RENDER_TYPE;
            }
        );
    }

    private static boolean isCosmicRenderType(RenderType rt) {
        return rt == AvaritiaShaders.COSMIC_RENDER_TYPE
            || rt == AvaritiaShaders.COSMIC_ITEM_AFTER_LEVEL_RENDER_TYPE
            || rt == AvaritiaShaders.COSMIC_HAND_AFTER_LEVEL_RENDER_TYPE
;
    }

    private ShaderRendererRegistry() {}
}
