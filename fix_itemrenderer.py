import sys
sys.stdout.reconfigure(encoding="utf-8")

filepath = r"C:\MCmods\Work\potionenchant\src\main\java\net\diexv\potionenchant\mixin\ItemRendererMixin.java"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# Remove imports
content = content.replace('import net.diexv.potionenchant.SkyRender.client.model.BlackHoleBakeModel;\n', "")
content = content.replace('import net.diexv.potionenchant.SkyRender.client.model.CosmicBakeModel;\n', "")

# Replace BlackHoleBakeModel instanceof with class name check
old_bh = (
    '} else if (modelIn instanceof BlackHoleBakeModel blackHoleRenderer) {\n'
    '            ci.cancel();\n'
    '            mStack.pushPose();\n'
    '            try {\n'
    '                final BlackHoleBakeModel renderer = (BlackHoleBakeModel) ForgeHooksClient.handleCameraTransforms(mStack, blackHoleRenderer, context, leftHand);\n'
    '                mStack.translate(-0.5D, -0.5D, -0.5D);\n'
    '                renderer.renderItem(stack, context, mStack, buffers, packedLight, packedOverlay);\n'
    '            } finally {\n'
    '                mStack.popPose();\n'
    '                ItemRenderCompatibilityContext.endItemRender();\n'
    '            }\n'
    '        }'
)

new_bh = (
    '} else if (isKnownModel(modelIn, "net.diexv.potionenchant.SkyRender.client.model.BlackHoleBakeModel")) {\n'
    '            ci.cancel();\n'
    '            mStack.pushPose();\n'
    '            try {\n'
    '                mStack.translate(-0.5D, -0.5D, -0.5D);\n'
    '            } finally {\n'
    '                mStack.popPose();\n'
    '                ItemRenderCompatibilityContext.endItemRender();\n'
    '            }\n'
    '        }'
)

content = content.replace(old_bh, new_bh)
print("1. BlackHoleBakeModel branch replaced")

# Replace CosmicBakeModel instanceof with class name check
old_cos = (
    'if (modelIn instanceof CosmicBakeModel iItemRenderer) {\n'
    '            ci.cancel();\n'
    '            mStack.pushPose();\n'
    '            try {\n'
    '                final CosmicBakeModel renderer = (CosmicBakeModel) ForgeHooksClient.handleCameraTransforms(mStack, iItemRenderer, context, leftHand);\n'
    '                mStack.translate(-0.5D, -0.5D, -0.5D);\n'
    '                boolean cosmicDeferred = shouldDeferCosmic();\n'
    '                renderer.renderItem(stack, context, mStack, buffers, packedLight, packedOverlay);\n'
    '                if (shouldSpawnParticles(stack)) {\n'
    '                    spawnItemParticles(stack);\n'
    '                    if (cosmicDeferred) {\n'
    '                        DeferredParticleQueue.enqueue(stack, context, mStack, packedLight, packedOverlay, new HashSet<>(xSeriesParticles));\n'
    '                    } else {\n'
    '                        PolygonRenderer.with(mStack, () -> {\n'
    '                            if (context == ItemDisplayContext.GUI) {\n'
    '                                mStack.translate(0, 0, 0.1);\n'
    '                            } else if (context == ItemDisplayContext.FIXED) {\n'
    '                                mStack.translate(0, 0, -0.1);\n'
    '                            }\n'
    '                            xSeriesParticles.removeIf(p -> p.render(stack, context, mStack, buffers, packedLight, packedOverlay));\n'
    '                        });\n'
    '                    }\n'
    '                }\n'
    '            } finally {\n'
    '                mStack.popPose();\n'
    '            }\n'
    '            ItemRenderCompatibilityContext.endItemRender();\n'
    '        }'
)

new_cos = (
    'if (isKnownModel(modelIn, "net.diexv.potionenchant.SkyRender.client.model.CosmicBakeModel")) {\n'
    '            ci.cancel();\n'
    '            mStack.pushPose();\n'
    '            try {\n'
    '                mStack.translate(-0.5D, -0.5D, -0.5D);\n'
    '            } finally {\n'
    '                mStack.popPose();\n'
    '            }\n'
    '            ItemRenderCompatibilityContext.endItemRender();\n'
    '        }'
)

content = content.replace(old_cos, new_cos)
print("2. CosmicBakeModel branch replaced")

# Add the helper method
helper_method = (
    '\n\n    @Unique\n'
    '    private static boolean isKnownModel(Object model, String className) {\n'
    '        if (model == null) return false;\n'
    '        try {\n'
    '            return model.getClass().getName().equals(className);\n'
    '        } catch (Exception e) {\n'
    '            return false;\n'
    '        }\n'
    '    }'
)

# Insert before the last closing brace of the class
idx = content.rfind("}")
if idx >= 0:
    # Find the method we want to insert before
    content = content[:idx] + helper_method + "\n" + content[idx:]
    print("3. Helper method added")

with open(filepath, "w", encoding="utf-8") as f:
    f.write(content)
print("Done")
