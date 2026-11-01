package android.content;

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Desktop shim for android.content.Intent: a plain value object. What an Intent does on the
 * desktop (open a URL, share, relaunch, broadcast) is decided by DesktopIntents. Java, like the
 * framework class, so Kotlin sees {@code type}, {@code action}, {@code clipData} ... as properties
 * while the setters still chain.
 */
public class Intent implements Parcelable, Cloneable {
  private String action;
  private Uri data;
  private String type;
  private int flags;
  private String pkg;
  private ComponentName component;
  private Bundle extras;
  private Set<String> categories;
  private ClipData clipData;

  public Intent() {}

  public Intent(String action) {
    this.action = action;
  }

  public Intent(String action, Uri uri) {
    this.action = action;
    this.data = uri;
  }

  public Intent(Context packageContext, Class<?> cls) {
    this.component = new ComponentName(packageContext, cls);
  }

  public Intent(String action, Uri uri, Context packageContext, Class<?> cls) {
    this(action, uri);
    this.component = new ComponentName(packageContext, cls);
  }

  public Intent(Intent o) {
    action = o.action;
    data = o.data;
    type = o.type;
    flags = o.flags;
    pkg = o.pkg;
    component = o.component;
    extras = o.extras == null ? null : new Bundle(o.extras);
    categories = o.categories == null ? null : new LinkedHashSet<>(o.categories);
    clipData = o.clipData;
  }

  public String getAction() { return action; }
  public Intent setAction(String action) { this.action = action; return this; }
  public Uri getData() { return data; }
  public Intent setData(Uri data) { this.data = data; return this; }
  public String getDataString() { return data == null ? null : data.toString(); }
  public String getType() { return type; }
  public Intent setType(String type) { this.type = type; return this; }
  public Intent setDataAndType(Uri data, String type) { this.data = data; this.type = type; return this; }
  public int getFlags() { return flags; }
  public Intent setFlags(int flags) { this.flags = flags; return this; }
  public Intent addFlags(int flags) { this.flags |= flags; return this; }
  public void removeFlags(int flags) { this.flags &= ~flags; }
  public String getPackage() { return pkg; }
  public Intent setPackage(String packageName) { this.pkg = packageName; return this; }
  public ComponentName getComponent() { return component; }
  public Intent setComponent(ComponentName component) { this.component = component; return this; }
  public Intent setClassName(String packageName, String className) { component = new ComponentName(packageName, className); return this; }
  public Intent setClass(Context packageContext, Class<?> cls) { component = new ComponentName(packageContext, cls); return this; }
  public Bundle getExtras() { return extras; }
  public Set<String> getCategories() { return categories; }
  public ClipData getClipData() { return clipData; }
  public void setClipData(ClipData clip) { this.clipData = clip; }

  public Intent addCategory(String category) {
    if (categories == null) categories = new LinkedHashSet<>();
    categories.add(category);
    return this;
  }

  public void removeCategory(String category) {
    if (categories != null) categories.remove(category);
  }

  public boolean hasCategory(String category) { return categories != null && categories.contains(category); }

  private Bundle bundle() {
    if (extras == null) extras = new Bundle();
    return extras;
  }

  public Intent putExtra(String name, String value) { bundle().putString(name, value); return this; }
  public Intent putExtra(String name, CharSequence value) { bundle().putCharSequence(name, value); return this; }
  public Intent putExtra(String name, int value) { bundle().putInt(name, value); return this; }
  public Intent putExtra(String name, long value) { bundle().putLong(name, value); return this; }
  public Intent putExtra(String name, boolean value) { bundle().putBoolean(name, value); return this; }
  public Intent putExtra(String name, int[] value) { bundle().putIntArray(name, value); return this; }
  public Intent putExtra(String name, String[] value) { bundle().putStringArray(name, value); return this; }
  public Intent putExtra(String name, Bundle value) { bundle().putBundle(name, value); return this; }
  public Intent putExtra(String name, Parcelable value) { bundle().putParcelable(name, value); return this; }
  public Intent putExtra(String name, java.io.Serializable value) { bundle().putSerializable(name, value); return this; }
  public Intent putParcelableArrayListExtra(String name, ArrayList<? extends Parcelable> value) { bundle().putParcelableArrayList(name, value); return this; }
  public Intent putStringArrayListExtra(String name, ArrayList<String> value) { bundle().putStringArrayList(name, value); return this; }
  public Intent putExtras(Bundle extras) { bundle().putAll(extras); return this; }
  public Intent putExtras(Intent src) { if (src.extras != null) bundle().putAll(src.extras); return this; }
  public boolean hasExtra(String name) { return extras != null && extras.containsKey(name); }
  public String getStringExtra(String name) { return extras == null ? null : extras.getString(name); }
  public CharSequence getCharSequenceExtra(String name) { return extras == null ? null : extras.getCharSequence(name); }
  public int getIntExtra(String name, int defaultValue) { return extras == null ? defaultValue : extras.getInt(name, defaultValue); }
  public long getLongExtra(String name, long defaultValue) { return extras == null ? defaultValue : extras.getLong(name, defaultValue); }
  public boolean getBooleanExtra(String name, boolean defaultValue) { return extras == null ? defaultValue : extras.getBoolean(name, defaultValue); }
  public int[] getIntArrayExtra(String name) { return extras == null ? null : extras.getIntArray(name); }
  public Bundle getBundleExtra(String name) { return extras == null ? null : extras.getBundle(name); }
  public ArrayList<String> getStringArrayListExtra(String name) { return extras == null ? null : extras.getStringArrayList(name); }
  public <T extends Parcelable> T getParcelableExtra(String name) { return extras == null ? null : extras.<T>getParcelable(name); }
  public <T> T getParcelableExtra(String name, Class<T> clazz) { return extras == null ? null : extras.getParcelable(name, clazz); }
  public <T extends Parcelable> ArrayList<T> getParcelableArrayListExtra(String name) { return extras == null ? null : extras.<T>getParcelableArrayList(name); }
  public void removeExtra(String name) { if (extras != null) extras.remove(name); }

  @Override
  public Object clone() { return new Intent(this); }

  @Override
  public String toString() {
    return "Intent { act=" + action + " dat=" + data + " typ=" + type + " flg=0x" + Integer.toHexString(flags)
        + " pkg=" + pkg + " cmp=" + component + " cat=" + categories + " extras=" + extras + " }";
  }

  public static Intent createChooser(Intent target, CharSequence title) {
    Intent chooser = new Intent(ACTION_CHOOSER).putExtra(EXTRA_INTENT, target);
    if (title != null) chooser.putExtra(EXTRA_TITLE, title);
    return chooser;
  }

  public static final String ACTION_MAIN = "android.intent.action.MAIN";
  public static final String ACTION_VIEW = "android.intent.action.VIEW";
  public static final String ACTION_SEND = "android.intent.action.SEND";
  public static final String ACTION_SEND_MULTIPLE = "android.intent.action.SEND_MULTIPLE";
  public static final String ACTION_SENDTO = "android.intent.action.SENDTO";
  public static final String ACTION_CHOOSER = "android.intent.action.CHOOSER";
  public static final String ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
  public static final String ACTION_BATTERY_CHANGED = "android.intent.action.BATTERY_CHANGED";
  public static final String ACTION_OPEN_DOCUMENT = "android.intent.action.OPEN_DOCUMENT";
  public static final String ACTION_OPEN_DOCUMENT_TREE = "android.intent.action.OPEN_DOCUMENT_TREE";
  public static final String ACTION_CREATE_DOCUMENT = "android.intent.action.CREATE_DOCUMENT";

  public static final String CATEGORY_LAUNCHER = "android.intent.category.LAUNCHER";
  public static final String CATEGORY_BROWSABLE = "android.intent.category.BROWSABLE";
  public static final String CATEGORY_DEFAULT = "android.intent.category.DEFAULT";
  public static final String CATEGORY_OPENABLE = "android.intent.category.OPENABLE";

  public static final String EXTRA_TEXT = "android.intent.extra.TEXT";
  public static final String EXTRA_SUBJECT = "android.intent.extra.SUBJECT";
  public static final String EXTRA_STREAM = "android.intent.extra.STREAM";
  public static final String EXTRA_INTENT = "android.intent.extra.INTENT";
  public static final String EXTRA_TITLE = "android.intent.extra.TITLE";
  public static final String EXTRA_MIME_TYPES = "android.intent.extra.MIME_TYPES";
  public static final String EXTRA_ALLOW_MULTIPLE = "android.intent.extra.ALLOW_MULTIPLE";
  public static final String EXTRA_PACKAGE_NAME = "android.intent.extra.PACKAGE_NAME";

  public static final int FLAG_GRANT_READ_URI_PERMISSION = 0x00000001;
  public static final int FLAG_GRANT_WRITE_URI_PERMISSION = 0x00000002;
  public static final int FLAG_GRANT_PERSISTABLE_URI_PERMISSION = 0x00000040;
  public static final int FLAG_ACTIVITY_SINGLE_TOP = 0x20000000;
  public static final int FLAG_ACTIVITY_NEW_TASK = 0x10000000;
  public static final int FLAG_ACTIVITY_CLEAR_TOP = 0x04000000;
  public static final int FLAG_ACTIVITY_NO_HISTORY = 0x40000000;
  public static final int FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY = 0x00100000;
  public static final int FLAG_ACTIVITY_CLEAR_TASK = 0x00008000;
  public static final int FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS = 0x00800000;
}
