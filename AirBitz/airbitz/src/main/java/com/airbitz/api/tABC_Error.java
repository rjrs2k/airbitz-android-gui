/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 3.0.2
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package com.airbitz.api;

public class tABC_Error {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  protected tABC_Error(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(tABC_Error obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        coreJNI.delete_tABC_Error(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public void setCode(tABC_CC value) {
    coreJNI.tABC_Error_code_set(swigCPtr, this, value.swigValue());
  }

  public tABC_CC getCode() {
    return tABC_CC.swigToEnum(coreJNI.tABC_Error_code_get(swigCPtr, this));
  }

  public void setSzDescription(String value) {
    coreJNI.tABC_Error_szDescription_set(swigCPtr, this, value);
  }

  public String getSzDescription() {
    return coreJNI.tABC_Error_szDescription_get(swigCPtr, this);
  }

  public void setSzSourceFunc(String value) {
    coreJNI.tABC_Error_szSourceFunc_set(swigCPtr, this, value);
  }

  public String getSzSourceFunc() {
    return coreJNI.tABC_Error_szSourceFunc_get(swigCPtr, this);
  }

  public void setSzSourceFile(String value) {
    coreJNI.tABC_Error_szSourceFile_set(swigCPtr, this, value);
  }

  public String getSzSourceFile() {
    return coreJNI.tABC_Error_szSourceFile_get(swigCPtr, this);
  }

  public void setNSourceLine(int value) {
    coreJNI.tABC_Error_nSourceLine_set(swigCPtr, this, value);
  }

  public int getNSourceLine() {
    return coreJNI.tABC_Error_nSourceLine_get(swigCPtr, this);
  }

  public tABC_Error() {
    this(coreJNI.new_tABC_Error(), true);
  }

}
