package net.diexv.potionenchant.event;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Optional;
import java.util.Random;

public class DiexvEventCore {
	public static Random RANDOM = new Random();

	public static int[] getPngDimensions(ResourceLocation location) {
		int width = 0;
		int height = 0;
		if (Minecraft.getInstance().getResourceManager() == null) {
			System.err.println("Resource Manager is not available.");
			return new int[] { width, height };
		}
		Optional<Resource> optionalResource = Minecraft.getInstance().getResourceManager().getResource(location);
		try {
			if (optionalResource.isPresent()) {
				Resource resource = optionalResource.get();
				try (InputStream inputStream = resource.open()) {
					BufferedImage image = ImageIO.read(inputStream);
					if (image != null) {
						width = image.getWidth();
						height = image.getHeight();
					}
				}
			} else {
				System.err.println("Resource not found: " + location);
			}
		} catch (Exception e) {
			// 静默处理异常
		}
		return new int[] { width, height };
	}

	public static float randfloat(float min, float max) {
		return min + RANDOM.nextFloat() * (max - min);
	}
}
