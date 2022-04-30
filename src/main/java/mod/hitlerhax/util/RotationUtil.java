package mod.hitlerhax.util;

import java.util.Comparator;
import java.util.concurrent.LinkedBlockingQueue;

import mod.hitlerhax.event.events.HtlrEventPacket;
import mod.hitlerhax.event.events.HtlrEventRotation;
import mod.hitlerhax.misc.Rotation;
import mod.hitlerhax.misc.Rotation.RotationMode;
import mod.hitlerhax.misc.RotationPriority;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class RotationUtil {

	public RotationUtil() {
		MinecraftForge.EVENT_BUS.register(this);
	}

	public static final LinkedBlockingQueue<Rotation> rotationQueue = new LinkedBlockingQueue<>();
	public static Rotation serverRotation = null;
	public static Rotation currentRotation = null;

	public static final float yawleftOver = 0;
	public static final float pitchleftOver = 0;

	public static int tick = 5;

	public static Vec2f getRotationTo(AxisAlignedBB box) {
		EntityPlayerSP player = Minecraft.getMinecraft().player;
		if (player == null) {
			return Vec2f.ZERO;
		}

		Vec3d eyePos = player.getPositionEyes(1.0f);

		if (player.getEntityBoundingBox().intersects(box)) {
			return getRotationTo(eyePos, box.getCenter());
		}

		double x = MathHelper.clamp(eyePos.x, box.minX, box.maxX);
		double y = MathHelper.clamp(eyePos.y, box.minY, box.maxY);
		double z = MathHelper.clamp(eyePos.z, box.minZ, box.maxZ);

		return getRotationTo(eyePos, new Vec3d(x, y, z));
	}

	public static Vec2f getRotationTo(Vec3d posTo) {
		EntityPlayerSP player = Minecraft.getMinecraft().player;
		return player != null ? getRotationTo(player.getPositionEyes(1.0f), posTo) : Vec2f.ZERO;
	}

	public static Vec2f getRotationTo(Vec3d posTo, EntityPlayer player) {
		return player != null ? getRotationTo(player.getPositionEyes(1.0f), posTo) : Vec2f.ZERO;
	}

	public static Vec2f getRotationTo(Vec3d posFrom, Vec3d posTo) {
		return getRotationFromVec(posTo.subtract(posFrom));
	}

	public static Vec2f getRotationFromVec(Vec3d vec) {
		double lengthXZ = Math.hypot(vec.x, vec.z);
		double yaw = normalizeAngle(Math.toDegrees(Math.atan2(vec.z, vec.x)) - 90.0);
		double pitch = normalizeAngle(Math.toDegrees(-Math.atan2(vec.y, lengthXZ)));

		return new Vec2f((float) yaw, (float) pitch);
	}

	public static double normalizeAngle(double angle) {
		angle %= 360.0;

		if (angle >= 180.0) {
			angle -= 360.0;
		}

		if (angle < -180.0) {
			angle += 360.0;
		}

		return angle;
	}

	public static float normalizeAngle(float angle) {
		angle %= 360.0f;

		if (angle >= 180.0f) {
			angle -= 360.0f;
		}

		if (angle < -180.0f) {
			angle += 360.0f;
		}

		return angle;
	}

	@SubscribeEvent
	public void onUpdate(TickEvent.ClientTickEvent event) {
		rotationQueue.stream().sorted(Comparator.comparing(rotation -> rotation.rotationPriority.getPriority()));

		if (currentRotation != null)
			currentRotation = null;

		if (!rotationQueue.isEmpty()) {
			currentRotation = rotationQueue.poll();
			currentRotation.updateRotations();
		}

		tick++;
	}

	@SubscribeEvent
	public void onRotate(HtlrEventRotation event) {
		try {
			if (currentRotation != null && currentRotation.mode.equals(RotationMode.Packet)) {
				event.setCanceled(true);

				if (tick == 1) {
					event.setYaw(currentRotation.yaw + yawleftOver);
					event.setPitch(currentRotation.pitch + pitchleftOver);
				}

				else {
					event.setYaw(currentRotation.yaw);
					event.setPitch(currentRotation.pitch);
				}
			}
		} catch (Exception ignored) {

		}
	}

	@SubscribeEvent
	public void onPacketSend(HtlrEventPacket.SendPacket event) {
		if (currentRotation != null && !rotationQueue.isEmpty() && event.get_packet() instanceof CPacketPlayer) {
			if (((CPacketPlayer) event.get_packet()).rotating)
				serverRotation = new Rotation(((CPacketPlayer) event.get_packet()).yaw,
						((CPacketPlayer) event.get_packet()).pitch, RotationMode.Packet, RotationPriority.Lowest);
		}
	}

}