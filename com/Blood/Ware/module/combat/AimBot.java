package com.Blood.Ware.module.combat;

import com.Blood.Ware.BloodWare;
import com.Blood.Ware.manager.FriendManager;
import com.Blood.Ware.module.Category;
import com.Blood.Ware.module.Module;
import com.Blood.Ware.settings.Setting;
import com.Blood.Ware.utils.FovUtils;
import com.Blood.Ware.utils.TimerUtils;
import java.awt.Color;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Post;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class AimBot extends Module {
   public float[] facing;
   public String[] gunlist = new String[]{""};
   public TimerUtils timer = new TimerUtils();
   public TimerUtils timer2 = new TimerUtils();

   public AimBot() {
      super("AimBot", Category.COMBAT);
      BloodWare.instance.settingsManager.rSetting(new Setting("Range", this, 300.0D, 0.0D, 300.0D, true));
      BloodWare.instance.settingsManager.rSetting(new Setting("Predict", this, 6.1D, 0.0D, 7.0D, false));
      BloodWare.instance.settingsManager.rSetting(new Setting("VPredict", this, 300.0D, 0.0D, 300.0D, true));
      BloodWare.instance.settingsManager.rSetting(new Setting("BAim", this, 0.0D, 0.0D, 10.0D, false));
      BloodWare.instance.settingsManager.rSetting(new Setting("FOV", this, 48.0D, 1.0D, 360.0D, true));
      BloodWare.instance.settingsManager.rSetting(new Setting("Wall", this, false));
      BloodWare.instance.settingsManager.rSetting(new Setting("Smooth", this, 5.0D, 1.0D, 20.0D, true));
   }

   public float[] getPredict(Entity e) {
      float VPredict = (float)BloodWare.instance.settingsManager.getSettingByName((Module)this, "VPredict").getValDouble();
      float Predict = (float)BloodWare.instance.settingsManager.getSettingByName((Module)this, "Predict").getValDouble();
      float Range = (float)BloodWare.instance.settingsManager.getSettingByName((Module)this, "Range").getValDouble();
      float smooth = (float)BloodWare.instance.settingsManager.getSettingByName((Module)this, "Smooth").getValDouble();
      double n = (double)mc.getRenderPartialTicks();
      double d = e.lastTickPosX + (e.posX - e.lastTickPosX) * n;
      double d2 = e.lastTickPosY + (e.posY - e.lastTickPosY) * n;
      double d3 = e.lastTickPosZ + (e.posZ - e.lastTickPosZ) * n;
      double xDiff = e.posX - e.prevPosX;
      double zDiff = e.posZ - e.prevPosZ;
      float predict = Predict + this.getDistance(e) / Range;
      double WillPosX = d + xDiff * (double)predict;
      double WillPosZ = d3 + zDiff * (double)predict;
      double WillPosY;
      if (VPredict != 0.0F) {
         WillPosY = d2 + (double)(this.getDistance(e) / VPredict);
      } else {
         WillPosY = d2;
      }

      return new float[]{(float)WillPosX, (float)WillPosZ, (float)WillPosY};
   }

   public static float[] faceHead(float posX, float posY, float posZ, float p_706252, float p_706253, boolean miss) {
      double offset = BloodWare.instance.settingsManager.getSettingByName(BloodWare.moduleManager.getModule("AimBot"), "BAim").getValDouble();
      double var4 = (double)posX - Minecraft.getMinecraft().player.posX;
      double var5 = (double)posZ - Minecraft.getMinecraft().player.posZ;
      double var6 = (double)posY + 1.86D - (Minecraft.getMinecraft().player.posY + (double)Minecraft.getMinecraft().player.getEyeHeight());
      if (offset == -1.0D) {
         var6 = (double)posY + 1.86D - (Minecraft.getMinecraft().player.posY + (double)Minecraft.getMinecraft().player.getEyeHeight());
      } else {
         float distance = getDistance(new BlockPos((double)posX, (double)posY, (double)posZ));
         if ((double)distance <= offset) {
            var6 = (double)posY + 1.6D - (Minecraft.getMinecraft().player.posY + (double)Minecraft.getMinecraft().player.getEyeHeight());
         }
      }

      double var7 = (double)MathHelper.sqrt(var4 * var4 + var5 * var5);
      float var8 = (float)(Math.atan2(var5, var4) * 180.0D / 3.141592653589793D) - 90.0F;
      float var9 = (float)(-(Math.atan2(var6 - 0.15D, var7) * 180.0D / 3.141592653589793D));
      float f = Minecraft.getMinecraft().gameSettings.mouseSensitivity * 0.6F + 0.2F;
      float gcd = f * f * f * 1.2F;
      float pitch = updateRotation(Minecraft.getMinecraft().player.rotationPitch, var9, p_706253);
      float yaw = updateRotation(Minecraft.getMinecraft().player.rotationYaw, var8, p_706252);
      yaw -= yaw % gcd;
      pitch -= pitch % gcd;
      return new float[]{yaw, pitch};
   }

   public static float updateRotation(float current, float intended, float speed) {
      float f = MathHelper.wrapDegrees(intended - current);
      if (f > speed) {
         f = speed;
      }

      if (f < -speed) {
         f = -speed;
      }

      return current + f;
   }

   private boolean lambdagetTarget(Entity entity) {
      return this.attackCheck(entity);
   }

   public Entity getTarget() throws Throwable {
      if (mc.player != null && !mc.player.isDead) {
         List list = (List)mc.world.loadedEntityList.stream().filter((entity) -> {
            return entity != mc.player;
         }).filter((entity) -> {
            return !entity.isDead;
         }).filter(this::lambdagetTarget).sorted(Comparator.comparing(FovUtils::getDistanceFromMouse)).collect(Collectors.toList());
         return list.size() > 0 ? (Entity)list.get(0) : null;
      } else {
         return null;
      }
   }

   public boolean attackCheck(Entity target) {
      boolean Walls = BloodWare.instance.settingsManager.getSettingByName((Module)this, "Wall").getValBoolean();
      if (Walls) {
         return target instanceof EntityPlayer && !FriendManager.FRIENDS.contains(target.getName());
      } else {
         return !Walls && mc.player.canEntityBeSeen(target) && target instanceof EntityPlayer && !FriendManager.FRIENDS.contains(target.getName());
      }
   }

   private float lerp(float start, float end, float percent) {
      return start + percent * (end - start);
   }

   @SubscribeEvent
   public void onLivingUpdate(LivingUpdateEvent e) throws Throwable {
      boolean Wall = BloodWare.instance.settingsManager.getSettingByName((Module)this, "Wall").getValBoolean();
      float Range = (float)BloodWare.instance.settingsManager.getSettingByName((Module)this, "Range").getValDouble();
      EntityPlayer target = (EntityPlayer)this.getTarget();
      if (target != null && !FriendManager.FRIENDS.contains(target.getName()) && FovUtils.isInAttackFOV(target, (int)BloodWare.instance.settingsManager.getSettingByName((Module)this, "FOV").getValDouble())) {
         this.facing = this.getPredict(target);
         this.facing = faceHead(this.facing[0], this.facing[2], this.facing[1], 360.0F, 360.0F, false);
         if (!BloodWare.instance.settingsManager.getSettingByName((Module)this, "Smooth").getValBoolean() && this.timer2.isDelay(1L)) {
            float lerpedYaw = this.lerp(mc.player.rotationYaw, this.facing[0], 0.7F);
            float lerpedPitch = this.lerp(mc.player.rotationPitch, this.facing[1], 0.7F);
            mc.player.rotationYaw = lerpedYaw;
            mc.player.rotationPitch = lerpedPitch;
            this.timer2.setLastMS();
         }
      }

   }

   private float getDistance(Entity entityIn) {
      float f = (float)(mc.player.posX - entityIn.posX);
      float f2 = (float)(mc.player.posZ - entityIn.posZ);
      return MathHelper.sqrt(f * f + f2 * f2);
   }

   private static float getDistance(BlockPos blockPos) {
      float f = (float)(mc.player.posX - (double)blockPos.func_177958_n());
      float f2 = (float)(mc.player.posZ - (double)blockPos.func_177952_p());
      return MathHelper.sqrt(f * f + f2 * f2);
   }

   @SubscribeEvent
   public void onRender(Post event) {
      double p = BloodWare.instance.settingsManager.getSettingByName((Module)this, "FOV").getValDouble() / (double)Minecraft.getMinecraft().gameSettings.fovSetting;
      ScaledResolution scaledResolution = event.getResolution();
      drawCircle228((float)(scaledResolution.getScaledWidth() / 2), (float)(scaledResolution.getScaledHeight() / 2), (float)((int)(p * 485.0D)), Color.RED.getRGB(), 360, 0.5F);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public static void drawCircle228(float n, float n2, float n3, int n4, int n5, float width) {
      float n6 = (float)(n4 >> 24 & 255) / 255.0F;
      float n7 = (float)(n4 >> 16 & 255) / 255.0F;
      float n8 = (float)(n4 >> 8 & 255) / 255.0F;
      float n9 = (float)(n4 & 255) / 255.0F;
      boolean glIsEnabled = GL11.glIsEnabled(3042);
      boolean glIsEnabled2 = GL11.glIsEnabled(2848);
      boolean glIsEnabled3 = GL11.glIsEnabled(3553);
      if (!glIsEnabled) {
         GL11.glEnable(3042);
      }

      if (!glIsEnabled2) {
         GL11.glEnable(2848);
      }

      if (glIsEnabled3) {
         GL11.glDisable(3553);
      }

      GL11.glEnable(2848);
      GL11.glBlendFunc(770, 771);
      GL11.glColor4f(n7, n8, n9, n6);
      GL11.glLineWidth(width);
      GL11.glBegin(3);

      for(int i = 0; i <= n5; ++i) {
         GL11.glVertex2d((double)n + Math.sin((double)i * 3.141592653589793D / 180.0D) * (double)n3, (double)n2 + Math.cos((double)i * 3.141592653589793D / 180.0D) * (double)n3);
      }

      GL11.glEnd();
      GL11.glDisable(2848);
      if (glIsEnabled3) {
         GL11.glEnable(3553);
      }

      if (!glIsEnabled2) {
         GL11.glDisable(2848);
      }

      if (!glIsEnabled) {
         GL11.glDisable(3042);
      }

   }
}
