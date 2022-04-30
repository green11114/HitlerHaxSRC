package mod.hitlerhax.module.modules.movement;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import mod.hitlerhax.Client;
import mod.hitlerhax.Main;
import mod.hitlerhax.event.events.HtlrEventPacket;
import mod.hitlerhax.event.events.HtlrEventPlayerTravel;
import mod.hitlerhax.module.Category;
import mod.hitlerhax.module.Module;
import mod.hitlerhax.setting.settings.BooleanSetting;
import mod.hitlerhax.setting.settings.FloatSetting;
import mod.hitlerhax.setting.settings.IntSetting;
import mod.hitlerhax.setting.settings.ModeSetting;
import mod.hitlerhax.util.MathUtil;
import mod.hitlerhax.util.Timer;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketEntityAction.Action;
import net.minecraft.network.play.client.CPacketPlayer;

public class ElytraFlight extends Module {
	public ModeSetting mode = new ModeSetting("Mode", this, "Normal", "Normal", "Tarzan", "Packet", "Superior",
			"Control");
	public final FloatSetting speed = new FloatSetting("Speed", this, 1.82f);
	public final FloatSetting DownSpeed = new FloatSetting("DownSpeed", this, 1.82f);
	public final FloatSetting GlideSpeed = new FloatSetting("GlideSpeed", this, 1f);
	public final FloatSetting UpSpeed = new FloatSetting("UpSpeed", this, 2.0f);
	public final BooleanSetting Accelerate = new BooleanSetting("Accelerate", this, true);
	public final IntSetting vAccelerationTimer = new IntSetting("Timer", this, 1000);
	public final FloatSetting RotationPitch = new FloatSetting("RotationPitch", this, 0.0f);
	public final BooleanSetting CancelInWater = new BooleanSetting("CancelInWater", this, true);
	public final IntSetting CancelAtHeight = new IntSetting("CancelAtHeight", this, 5);
	public final BooleanSetting InstantFly = new BooleanSetting("InstantFly", this, true);
	public final BooleanSetting EquipElytra = new BooleanSetting("EquipElytra", this, false);
	public final BooleanSetting PitchSpoof = new BooleanSetting("PitchSpoof", this, false);

	private Timer AccelerationTimer = new Timer();
	private Timer AccelerationResetTimer = new Timer();
	private Timer InstantFlyTimer = new Timer();
	private boolean SendMessage = false;
	private Flight flight = null;

	public ElytraFlight() {
		super("ElytraFly", "Allows you to fly with elytra on 2b2t", Category.MOVEMENT);
		addSetting(mode);
		addSetting(speed);
		addSetting(DownSpeed);
		addSetting(GlideSpeed);
		addSetting(UpSpeed);
		addSetting(Accelerate);
		addSetting(vAccelerationTimer);
		addSetting(RotationPitch);
		addSetting(CancelInWater);
		addSetting(CancelAtHeight);
		addSetting(InstantFly);
		addSetting(EquipElytra);
		addSetting(PitchSpoof);
	}

	private int ElytraSlot = -1;

	@Override
	public void onEnable() {
		super.onEnable();

		flight = (Flight) Main.moduleManager.getModule("Flight");

		ElytraSlot = -1;

		if (EquipElytra.enabled) {
			if (mc.player != null
					&& mc.player.getItemStackFromSlot(EntityEquipmentSlot.CHEST).getItem() != Items.ELYTRA) {
				for (int l_I = 0; l_I < 44; ++l_I) {
					ItemStack l_Stack = mc.player.inventory.getStackInSlot(l_I);

					if (l_Stack.isEmpty() || l_Stack.getItem() != Items.ELYTRA)
						continue;

					l_Stack.getItem();

					ElytraSlot = l_I;
					break;
				}

				if (ElytraSlot != -1) {
					boolean l_HasArmorAtChest = mc.player.getItemStackFromSlot(EntityEquipmentSlot.CHEST)
							.getItem() != Items.AIR;

					mc.playerController.windowClick(mc.player.inventoryContainer.windowId, ElytraSlot, 0,
							ClickType.PICKUP, mc.player);
					mc.playerController.windowClick(mc.player.inventoryContainer.windowId, 6, 0, ClickType.PICKUP,
							mc.player);

					if (l_HasArmorAtChest)
						mc.playerController.windowClick(mc.player.inventoryContainer.windowId, ElytraSlot, 0,
								ClickType.PICKUP, mc.player);
				}
			}
		}
	}

	@Override
	public void onDisable() {
		super.onDisable();

		if (mc.player == null)
			return;

		if (ElytraSlot != -1) {
			boolean l_HasItem = !mc.player.inventory.getStackInSlot(ElytraSlot).isEmpty()
					|| mc.player.inventory.getStackInSlot(ElytraSlot).getItem() != Items.AIR;

			mc.playerController.windowClick(mc.player.inventoryContainer.windowId, 6, 0, ClickType.PICKUP, mc.player);
			mc.playerController.windowClick(mc.player.inventoryContainer.windowId, ElytraSlot, 0, ClickType.PICKUP,
					mc.player);

			if (l_HasItem)
				mc.playerController.windowClick(mc.player.inventoryContainer.windowId, 6, 0, ClickType.PICKUP,
						mc.player);
		}
	}

	@EventHandler
	private Listener<HtlrEventPlayerTravel> OnTravel = new Listener<>(p_Event -> {
		if (mc.player == null || flight.isEnabled()) /// < Ignore if Flight is on: ex flat flying
			return;

		/// Player must be wearing an elytra.
		if (mc.player.getItemStackFromSlot(EntityEquipmentSlot.CHEST).getItem() != Items.ELYTRA)
			return;

		if (!mc.player.isElytraFlying()) {
			if (!mc.player.onGround && InstantFly.enabled) {
				if (!InstantFlyTimer.passed(1000))
					return;

				InstantFlyTimer.reset();

				mc.player.connection.sendPacket(new CPacketEntityAction(mc.player, Action.START_FALL_FLYING));
			}

			return;
		}

		switch (mode.getMode()) {
		case "Normal":
		case "Tarzan":
		case "Packet":
			HandleNormalModeElytra(p_Event);
			break;
		case "Superior":
			HandleImmediateModeElytra(p_Event);
			break;
		case "Control":
			HandleControlMode(p_Event);
			break;
		default:
			break;
		}
	});

	public void HandleNormalModeElytra(HtlrEventPlayerTravel p_Travel) {
		double l_YHeight = mc.player.posY;

		if (l_YHeight <= CancelAtHeight.getValue()) {
			if (!SendMessage) {
				Client.addChatMessage("WARNING, you must scaffold up or use fireworks, as YHeight <= CancelAtHeight!");
				SendMessage = true;
			}

			return;
		}

		boolean l_IsMoveKeyDown = mc.player.movementInput.moveForward > 0 || mc.player.movementInput.moveStrafe > 0;

		boolean l_CancelInWater = !mc.player.isInWater() && !mc.player.isInLava() && CancelInWater.enabled;

		if (mc.player.movementInput.jump) {
			p_Travel.cancel();
			Accelerate();
			return;
		}

		if (!l_IsMoveKeyDown) {
			AccelerationTimer.resetTimeSkipTo(-vAccelerationTimer.getValue());
		} else if ((mc.player.rotationPitch <= RotationPitch.getValue() || mode.getMode().equals("Tarzan"))
				&& l_CancelInWater) {
			if (Accelerate.enabled) {
				if (AccelerationTimer.passed(vAccelerationTimer.getValue())) {
					Accelerate();
					return;
				}
			}
			return;
		}

		p_Travel.cancel();
		Accelerate();
	}

	public void HandleImmediateModeElytra(HtlrEventPlayerTravel p_Travel) {
		if (mc.player.movementInput.jump) {
			double l_MotionSq = Math
					.sqrt(mc.player.motionX * mc.player.motionX + mc.player.motionZ * mc.player.motionZ);

			if (l_MotionSq > 1.0) {
				return;
			} else {
				double[] dir = MathUtil.directionSpeedNoForward(speed.getValue());

				mc.player.motionX = dir[0];
				mc.player.motionY = -(GlideSpeed.getValue() / 10000f);
				mc.player.motionZ = dir[1];
			}

			p_Travel.cancel();
			return;
		}

		mc.player.setVelocity(0, 0, 0);

		p_Travel.cancel();

		double[] dir = MathUtil.directionSpeed(speed.getValue());

		if (mc.player.movementInput.moveStrafe != 0 || mc.player.movementInput.moveForward != 0) {
			mc.player.motionX = dir[0];
			mc.player.motionY = -(GlideSpeed.getValue() / 10000f);
			mc.player.motionZ = dir[1];
		}

		if (mc.player.movementInput.sneak)
			mc.player.motionY = -DownSpeed.getValue();

		mc.player.prevLimbSwingAmount = 0;
		mc.player.limbSwingAmount = 0;
		mc.player.limbSwing = 0;
	}

	public void Accelerate() {
		if (AccelerationResetTimer.passed(vAccelerationTimer.getValue())) {
			AccelerationResetTimer.reset();
			AccelerationTimer.reset();
			SendMessage = false;
		}

		float l_Speed = this.speed.getValue();

		final double[] dir = MathUtil.directionSpeed(l_Speed);

		mc.player.motionY = -(GlideSpeed.getValue() / 10000f);

		if (mc.player.movementInput.moveStrafe != 0 || mc.player.movementInput.moveForward != 0) {
			mc.player.motionX = dir[0];
			mc.player.motionZ = dir[1];
		} else {
			mc.player.motionX = 0;
			mc.player.motionZ = 0;
		}

		if (mc.player.movementInput.sneak)
			mc.player.motionY = -DownSpeed.getValue();

		mc.player.prevLimbSwingAmount = 0;
		mc.player.limbSwingAmount = 0;
		mc.player.limbSwing = 0;
	}

	private void HandleControlMode(HtlrEventPlayerTravel p_Event) {
		final double[] dir = MathUtil.directionSpeed(speed.getValue());

		if (mc.player.movementInput.moveStrafe != 0 || mc.player.movementInput.moveForward != 0) {
			mc.player.motionX = dir[0];
			mc.player.motionZ = dir[1];

			mc.player.motionX -= (mc.player.motionX * (Math.abs(mc.player.rotationPitch) + 90) / 90)
					- mc.player.motionX;
			mc.player.motionZ -= (mc.player.motionZ * (Math.abs(mc.player.rotationPitch) + 90) / 90)
					- mc.player.motionZ;
		} else {
			mc.player.motionX = 0;
			mc.player.motionZ = 0;
		}

		mc.player.motionY = (-MathUtil.degToRad(mc.player.rotationPitch)) * mc.player.movementInput.moveForward;

		mc.player.prevLimbSwingAmount = 0;
		mc.player.limbSwingAmount = 0;
		mc.player.limbSwing = 0;
		p_Event.cancel();
	}

	@EventHandler
	private Listener<HtlrEventPacket.ReceivePacket> PacketEvent = new Listener<>(p_Event -> {
		if (p_Event.get_packet() instanceof CPacketPlayer && PitchSpoof.enabled) {
			if (!mc.player.isElytraFlying())
				return;

			if (p_Event.get_packet() instanceof CPacketPlayer.PositionRotation && PitchSpoof.enabled) {
				CPacketPlayer.PositionRotation rotation = (CPacketPlayer.PositionRotation) p_Event.get_packet();

				mc.getConnection()
						.sendPacket(new CPacketPlayer.Position(rotation.x, rotation.y, rotation.z, rotation.onGround));
				p_Event.cancel();
			} else if (p_Event.get_packet() instanceof CPacketPlayer.Rotation && PitchSpoof.enabled) {
				p_Event.cancel();
			}
		}
	});
}
