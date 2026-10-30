package physical_progression.client.screen;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/** Bounded history, selected by stable server record id rather than a changing list index. */
public final class TrainingHistoryPanel {
  private record Entry(int x, int y, int width, int height, long id) {}

  private final List<Entry> hits = new ArrayList<>();

  public void render(PanelContext p, long selected) {
    hits.clear();
    var records = p.data.attributes.getListOrEmpty("history");
    if (selected >= 0) {
      link(
          p,
          "‹ " + Component.translatable("ui.physical_progression.history_back").getString(),
          -1,
          18);
      CompoundTag detail = null;
      for (var entry : records)
        if (entry instanceof CompoundTag tag && tag.getLongOr("id", 0L) == selected) {
          detail = tag;
          break;
        }
      if (detail != null) TrainingPanel.details(p, detail);
      else p.text("ui.physical_progression.history_missing");
      return;
    }
    p.heading("ui.physical_progression.training_history");
    if (records.isEmpty()) {
      p.text("ui.physical_progression.no_session");
      return;
    }
    for (var entry : records) {
      var tag = (CompoundTag) entry;
      String title =
          "#"
              + tag.getLongOr("id", 0L)
              + "  "
              + Component.translatable(
                      "activity.physical_progression." + tag.getStringOr("activity", ""))
                  .getString();
      int rowY = p.y;
      link(p, title, tag.getLongOr("id", 0L), 44);
      double xp = 0;
      for (String key : tag.keySet()) if (key.endsWith("Xp")) xp += tag.getDoubleOr(key, 0.0);
      String summary =
          TrainingPanel.duration(tag.getIntOr("ticks", 0))
              + " · "
              + PanelContext.percent(tag.getDoubleOr("intensity", 0.0))
              + " · +"
              + PanelContext.number(xp)
              + " XP";
      p.g.text(Minecraft.getInstance().font, summary, p.x + 5, rowY + 16, 0xFFCCCCCC);
      String outcome =
          Component.translatable(
                  "outcome.physical_progression."
                      + (tag.contains("outcome") ? tag.getStringOr("outcome", "") : "legacy"))
              .getString();
      p.g.text(
          Minecraft.getInstance().font,
          TrainingPanel.completedAt(tag) + " · " + outcome,
          p.x + 5,
          rowY + 29,
          0xFFAAAAAA);
    }
  }

  private void link(PanelContext p, String label, long id, int height) {
    boolean hovered =
        p.mouseX >= p.x
            && p.mouseX < p.x + p.width
            && p.mouseY >= p.y
            && p.mouseY < p.y + height - 2;
    p.g.fill(p.x, p.y, p.x + p.width, p.y + height - 2, hovered ? 0xFF474747 : 0xFF343434);
    p.g.fill(p.x, p.y, p.x + 2, p.y + height - 2, hovered ? 0xFFFFD56A : 0xFF6D9D3C);
    var font = Minecraft.getInstance().font;
    p.g.text(
        font,
        font.plainSubstrByWidth(label, p.width - 22),
        p.x + 5,
        p.y + 4,
        hovered ? 0xFFF4D893 : 0xFFE3DFD2);
    p.g.text(font, ">", p.x + p.width - 12, p.y + 4, hovered ? 0xFFFFD56A : 0xFFBBBBBB);
    hits.add(new Entry(p.x, p.y, p.width, height - 2, id));
    p.y += height;
  }

  public Long click(double x, double y) {
    for (var hit : hits)
      if (x >= hit.x && x < hit.x + hit.width && y >= hit.y && y < hit.y + hit.height)
        return hit.id;
    return null;
  }
}
