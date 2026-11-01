package android.os;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Desktop shim for android.os.LocaleList: an immutable, ordered list of {@link Locale}s (Java, like the framework). */
public final class LocaleList {
  private static final LocaleList EMPTY = new LocaleList();
  private final List<Locale> locales;

  public LocaleList(Locale... list) {
    List<Locale> out = new ArrayList<>();
    for (Locale l : list) if (!out.contains(l)) out.add(l);
    locales = out;
  }

  public Locale get(int index) { return index >= 0 && index < locales.size() ? locales.get(index) : null; }
  public int size() { return locales.size(); }
  public boolean isEmpty() { return locales.isEmpty(); }
  public int indexOf(Locale locale) { return locales.indexOf(locale); }

  public String toLanguageTags() {
    StringBuilder b = new StringBuilder();
    for (Locale l : locales) { if (b.length() > 0) b.append(','); b.append(l.toLanguageTag()); }
    return b.toString();
  }

  @Override public boolean equals(Object o) { return o instanceof LocaleList && ((LocaleList) o).locales.equals(locales); }
  @Override public int hashCode() { return locales.hashCode(); }
  @Override public String toString() { return locales.toString(); }

  public static LocaleList getEmptyLocaleList() { return EMPTY; }

  public static LocaleList forLanguageTags(String list) {
    if (list == null || list.trim().isEmpty()) return EMPTY;
    return new LocaleList(Arrays.stream(list.split(",")).map(t -> Locale.forLanguageTag(t.trim())).toArray(Locale[]::new));
  }

  /** The process's default list: the app locale when one is set, else the system's. */
  public static LocaleList getDefault() { return new LocaleList(Locale.getDefault()); }

  public static LocaleList getAdjustedDefault() { return getDefault(); }
}
