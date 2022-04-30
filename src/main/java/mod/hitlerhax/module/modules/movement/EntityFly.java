package mod.hitlerhax.module.modules.movement;

import java.util.Objects;

import mod.hitlerhax.module.Category;
import mod.hitlerhax.module.Module;
import mod.hitlerhax.setting.settings.FloatSetting;
import mod.hitlerhax.setting.settings.IntSetting;
import mod.hitlerhax.setting.settings.ModeSetting;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.util.math.MathHelper;

public class EntityFly extends Module {
	public EntityFly() {
		super("EntityFly", "Lets your riding entity fly", Category.MOVEMENT);
		addSetting(mode);
		addSetting(multiplier);
		addSetting(glideHeight);
		addSetting(vSpeed);
		addSetting(hSpeed);
		addSetting(reduction);
		addSetting(jumpMovement);
		this.addSetting(hoverAmt);
		this.addSetting(hoverSpeed);
		addSetting(bSpeed);
		addSetting(bFreq);
		addSetting(swLimit);
	}

	final ModeSetting mode = new ModeSetting("Mode", this, "y-axis motionless", "y-axis motionless", "fall reduction",
			"packet", "HorseJump", "Sinwave abs", "Sinwave max");
	final FloatSetting multiplier = new FloatSetting("Multiplier", this, 1.0f);
	final FloatSetting glideHeight = new FloatSetting("Glide", this, 0.3f);
	final FloatSetting vSpeed = new FloatSetting("Packet VSpeed", this, 1.0f);
	final FloatSetting hSpeed = new FloatSetting("Packet HSpeed", this, 1.0f);
	final FloatSetting reduction = new FloatSetting("Fall Reduction", this, 0.03f);
	final IntSetting jumpMovement = new IntSetting("Horse Jump Power", this, 1);
	private final FloatSetting hoverAmt = new FloatSetting("Hover Amount", this, 0.0f);
	private final FloatSetting hoverSpeed = new FloatSetting("Hover Speed", this, 1.0f);
	FloatSetting bSpeed = new FloatSetting("Sinwave Speed", this, 10.0f);
	IntSetting bFreq = new IntSetting("Sinwave Frequency ms", this, 1000);
	IntSetting swLimit = new IntSetting("Sinwave height limit", this, 500);

	@Override
	public void onUpdate() {
		if (mc.world == null || mc.player == null || mc.player.ridingEntity == null) {
			return;
		}

		double[] pos = { mc.player.getRidingEntity().prevPosX, mc.player.getRidingEntity().prevPosY,
				mc.player.getRidingEntity().prevPosZ };
		switch (mode.getMode()) {
		case "y-axis motionless":
			mc.player.ridingEntity.motionY = 0;
			break;
		case "fall reduction":
			if (mc.player.ridingEntity.motionY < 0) {
				mc.player.ridingEntity.motionY += reduction.value;
				if (mc.player.ridingEntity.motionY > 0)
					mc.player.ridingEntity.motionY = 0;
			}
			break;
		case "packet":
			float player_speed = 0.2873f;
			float rotation_yaw = mc.player.getRidingEntity().rotationYaw;
			float move_forward = mc.player.movementInput.moveForward;
			float move_strafe = mc.player.movementInput.moveStrafe;
			if (mc.player.moveForward > 0) {
				pos[0] += ((move_forward * player_speed) * Math.cos(Math.toRadians((rotation_yaw + 90.0f)))
						+ (move_strafe * player_speed) * Math.sin(Math.toRadians((rotation_yaw + 90.0f))))
						* hSpeed.value;
				pos[2] += ((move_forward * player_speed) * Math.sin(Math.toRadians((rotation_yaw + 90.0f)))
						- (move_strafe * player_speed) * Math.cos(Math.toRadians((rotation_yaw + 90.0f))))
						* hSpeed.value;
			}

			double vert = 0;
			if (mc.player.movementInput.jump) {
				vert += 1d * vSpeed.value;
			} else if (mc.player.moveForward < 0.0f) {
				vert -= 1d * vSpeed.value;
			} else {
				mc.player.motionY = 0;
				mc.player.getRidingEntity().motionY = 0;
			}
			pos[1] += vert;

			mc.player.getRidingEntity().setPosition(pos[0], pos[1], pos[2]);
			mc.player.setPosition(pos[0], pos[1], pos[2]);
			final float yaw = YawRotationUtility();
			if (mc.player.movementInput.forwardKeyDown) {
				mc.player.getRidingEntity().motionX -= MathHelper.sin(yaw) * 0.017453292f * hSpeed.value;
				mc.player.getRidingEntity().motionZ += MathHelper.cos(yaw) * 0.017453292f * hSpeed.value;
				mc.player.motionX -= MathHelper.sin(yaw) * 0.017453292f * hSpeed.value;
				mc.player.motionZ += MathHelper.cos(yaw) * 0.017453292f * hSpeed.value;

				mc.player.connection
						.sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY, mc.player.posZ, false));
			}
			break;
		case "HorseJump":
			if (mc.player.isRidingHorse()) {
				((AbstractHorse) mc.player.ridingEntity).setJumpPower(this.jumpMovement.value);
			}
			break;
		case "Sinwave abs":
			if (bFreq.value != 0 && (System.currentTimeMillis() % bFreq.value) * 2 >= bFreq.value)
				if (mc.player.isRidingHorse()) {
					long time = System.currentTimeMillis();
					mc.player.motionY = bSpeed.value * Math.abs(Math.sin(bFreq.getValue() * time));
					mc.player.ridingEntity.motionY = bSpeed.value * Math.abs(Math.sin(bFreq.getValue() * time));
				}
		case "Sinwave max":
			if (bFreq.value != 0 && (System.currentTimeMillis() % bFreq.value) * 2 >= bFreq.value
					&& mc.player.posY < this.swLimit.value)
				if (mc.player.isRidingHorse()) {
					long time = System.currentTimeMillis();
					if (bSpeed.value * Math.max(0, Math.sin(bFreq.getValue() * time)) != 0) {
						mc.player.motionY = bSpeed.value * Math.max(0, Math.sin(bFreq.getValue() * time));
						mc.player.ridingEntity.motionY = bSpeed.value * Math.max(0, Math.sin(bFreq.getValue() * time));
					}
				}
		}
		if (hoverAmt.getValue() != 0.0f && mc.player.getRidingEntity() instanceof AbstractHorse) {
			long time = System.currentTimeMillis();
			mc.player.ridingEntity.posY += hoverAmt.getValue() * Math.sin(hoverSpeed.getValue() * time);
			mc.player.posY += hoverAmt.getValue() * Math.sin(hoverSpeed.getValue() * time);
		}
		if (mc.player.getRidingEntity() instanceof AbstractHorse) {
			mc.player.ridingEntity.posY -= glideHeight.value;
			mc.player.posY -= glideHeight.value;
		}
	}

	private float YawRotationUtility() {
		float rotationYaw = Objects.requireNonNull(mc.player.getRidingEntity()).rotationYaw;

		return rotationYaw * 0.017453292f;
	}
}