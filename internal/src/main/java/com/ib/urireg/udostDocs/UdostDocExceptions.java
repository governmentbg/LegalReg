package com.ib.urireg.udostDocs;

public class UdostDocExceptions {

    public static class MissingDataException extends RuntimeException {
        private static final long serialVersionUID = -5121616202710447595L;

        public MissingDataException(String methodName, String fieldName) {
            super("На метода " + methodName + " не е подаден параметър " + fieldName);
        }
    }
}
