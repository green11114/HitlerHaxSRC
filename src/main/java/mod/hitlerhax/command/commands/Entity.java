package mod.hitlerhax.command.commands;

import mod.hitlerhax.Client;
import mod.hitlerhax.command.Command;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.util.EnumHand;

public class Entity extends Command {
	@Override
	public String getAlias() {
		return "entity";
	}

	@Override
	public String getDescription() {
		return "interact with a set entity";
	}

	@Override
	public String getSyntax() {
		return ".entity setpointedentity | .entity clear | .entity inventory | .entity ride | .entity dismount | .entity desyncride";
	}

	private net.minecraft.entity.Entity e;

	@Override
	public void onCommand(String command, String[] args) {
		if (args[0].isEmpty()) {
			Client.addChatMessage("No arguments found");
			Client.addChatMessage(this.getSyntax());
		} else {
			switch (args[0]) {
			case "setpointedentity":
				e = mc.pointedEntity;
				break;
			case "desyncride":
				mc.player.startRiding(e);
				break;
			case "clear":
				e = null;
				break;
			case "inventory":
				if (e != null && e instanceof AbstractHorse) {
					mc.getConnection()
							.sendPacket(new CPacketEntityAction(mc.player, CPacketEntityAction.Action.START_SNEAKING));
					mc.getConnection().sendPacket(new CPacketUseEntity((AbstractHorse) e, EnumHand.MAIN_HAND));
					mc.getConnection()
							.sendPacket(new CPacketEntityAction(mc.player, CPacketEntityAction.Action.STOP_SNEAKING));
				}
				break;
			case "ride":
				mc.getConnection().sendPacket(new CPacketUseEntity((AbstractHorse) e, EnumHand.MAIN_HAND));
				break;
			case "dismount":
				mc.player.dismountRidingEntity();
				break;
			default:
				Client.addChatMessage("Invalid Argument(s)");
				Client.addChatMessage(this.getSyntax());
				break;
			}
		}
	}
}
