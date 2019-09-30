/*
 * Copyright (c) 2019 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.connector.network.translators.bedrock;

import com.flowpowered.math.vector.Vector3i;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerAction;
import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerState;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockFace;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerActionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPlaceBlockPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerStatePacket;
import com.nukkitx.protocol.bedrock.packet.PlayerActionPacket;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;

public class BedrockActionTranslator extends PacketTranslator<PlayerActionPacket> {

    @Override
    public void translate(PlayerActionPacket packet, GeyserSession session) {
        Entity entity = session.getPlayerEntity();
        if (entity == null)
            return;

        Vector3i vector = packet.getBlockPosition();
        Position position = new Position(vector.getX(), vector.getY(), vector.getZ());

        switch (packet.getAction()) {
            case RESPAWN:
                // Don't put anything here as respawn is already handled
                // in JavaPlayerSetHealthTranslator
                break;
            case START_GLIDE:
            case STOP_GLIDE:
                ClientPlayerStatePacket glidePacket = new ClientPlayerStatePacket((int) session.getPlayerEntity().getGeyserId(), PlayerState.START_ELYTRA_FLYING);
                session.getDownstream().getSession().send(glidePacket);
                break;
            case START_SNEAK:
                ClientPlayerStatePacket startSneakPacket = new ClientPlayerStatePacket((int) session.getPlayerEntity().getGeyserId(), PlayerState.START_SNEAKING);
                session.getDownstream().getSession().send(startSneakPacket);
                break;
            case STOP_SNEAK:
                ClientPlayerStatePacket stopSneakPacket = new ClientPlayerStatePacket((int) session.getPlayerEntity().getGeyserId(), PlayerState.STOP_SNEAKING);
                session.getDownstream().getSession().send(stopSneakPacket);
                break;
            case START_SPRINT:
                ClientPlayerStatePacket startSprintPacket = new ClientPlayerStatePacket((int) session.getPlayerEntity().getGeyserId(), PlayerState.START_SPRINTING);
                session.getDownstream().getSession().send(startSprintPacket);
                break;
            case STOP_SPRINT:
                ClientPlayerStatePacket stopSprintPacket = new ClientPlayerStatePacket((int) session.getPlayerEntity().getGeyserId(), PlayerState.STOP_SPRINTING);
                session.getDownstream().getSession().send(stopSprintPacket);
                break;
            case DROP_ITEM:
                ClientPlayerActionPacket dropItemPacket = new ClientPlayerActionPacket(PlayerAction.DROP_ITEM, position, BlockFace.values()[packet.getFace()]);
                session.getDownstream().getSession().send(dropItemPacket);
                break;

            case STOP_SLEEP:
                ClientPlayerStatePacket stopSleepingPacket = new ClientPlayerStatePacket((int) session.getPlayerEntity().getGeyserId(), PlayerState.LEAVE_BED);
                session.getDownstream().getSession().send(stopSleepingPacket);
                break;

            case BLOCK_INTERACT:
                ClientPlayerPlaceBlockPacket blockPacket = new ClientPlayerPlaceBlockPacket(position,
                        BlockFace.values()[packet.getFace()],
                        Hand.MAIN_HAND, 0, 0, 0, false);

                session.getDownstream().getSession().send(blockPacket);
                break;

            case START_BREAK:
                System.out.println("a");
                ClientPlayerActionPacket actionPacket = new ClientPlayerActionPacket(PlayerAction.START_DIGGING, position, BlockFace.values()[1]);
                session.getDownstream().getSession().send(actionPacket);

                session.setThreadStop(false);
                session.setBreaking(true);

                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        while (session.isThreadStop()) {
                            try {
                                Thread.sleep(1);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            if(session.isBreaking()) {
                                session.setBreaking(false);
                            } else {
                                ClientPlayerActionPacket actionPacket = new ClientPlayerActionPacket(PlayerAction.CANCEL_DIGGING, position, BlockFace.values()[1]);
                                session.getDownstream().getSession().send(actionPacket);
                            }
                        }
                    }
                };

                session.setBreakThread(thread);

                thread.start();

            case STOP_BREAK:
                System.out.println("b");
                session.getBreakThread().stop();
                break;

            case ABORT_BREAK:
                System.out.println("c");
                //ClientPlayerActionPacket actionPacket3 = new ClientPlayerActionPacket(PlayerAction.CANCEL_DIGGING, position, BlockFace.values()[1]);

                //session.getDownstream().getSession().send(actionPacket3);
                break;

            case CONTINUE_BREAK:
                System.out.println("d");

                session.setBreaking(true);

                break;
        }
    }
}