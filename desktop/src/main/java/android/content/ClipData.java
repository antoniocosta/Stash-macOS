package android.content;

import android.net.Uri;
import java.util.ArrayList;
import java.util.List;

/** Desktop shim for android.content.ClipData: text and URI items. */
public class ClipData {
  public static class Item {
    private final CharSequence text;
    private final Uri uri;

    public Item(CharSequence text) { this.text = text; this.uri = null; }
    public Item(Uri uri) { this.text = null; this.uri = uri; }
    public CharSequence getText() { return text; }
    public Uri getUri() { return uri; }
  }

  private final CharSequence label;
  private final List<Item> items = new ArrayList<>();

  private ClipData(CharSequence label, Item first) {
    this.label = label;
    items.add(first);
  }

  public CharSequence getLabel() { return label; }
  public int getItemCount() { return items.size(); }
  public Item getItemAt(int index) { return items.get(index); }
  public void addItem(Item item) { items.add(item); }
  /** First item's text (what the desktop clipboard carries). */
  public CharSequence getText() { return items.get(0).getText(); }

  public static ClipData newPlainText(CharSequence label, CharSequence text) { return new ClipData(label, new Item(text)); }
  public static ClipData newRawUri(CharSequence label, Uri uri) { return new ClipData(label, new Item(uri)); }
  public static ClipData newUri(ContentResolver resolver, CharSequence label, Uri uri) { return new ClipData(label, new Item(uri)); }
}
