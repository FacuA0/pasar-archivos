/*
 * Code taken from the directory-jvm library by dirs-dev (https://github.com/dirs-dev/directories-jvm)
 */

package dev.dirs;

public class UnsupportedOperatingSystemException extends UnsupportedOperationException {

  private static final long serialVersionUID = -6241121024431394902L;

  public UnsupportedOperatingSystemException(String message) {
    super(message);
  }

}
