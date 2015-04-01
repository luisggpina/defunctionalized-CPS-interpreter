package util;

public class List {
  public final static List EMPTY = new List(null, null);

  public final Object el;
  public final List   rest;

  private List(Object el, List rest) {
    this.el = el;
    this.rest = rest;
  }
  
  public List add(Object el) {
    return new List(el, this);
  }
}
