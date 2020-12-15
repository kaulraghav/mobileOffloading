package com.example.mcfinal.mcoffloading;

import android.content.Context;
import android.content.Intent;
import com.example.mcfinal.mcoffloading.MainActivity2;

public class MatrixMult {

    public Intent navigateResultScreen(String row1, String row2, String row3, String row4,
                                       String estimation1, String estimation2, String estimation3, Context context){
        Intent intent = new Intent(context, MainActivity2.class);
        intent.putExtra("row1", row1);
        intent.putExtra("row2", row2);
        intent.putExtra("row3", row3);
        intent.putExtra("row4", row4);
        intent.putExtra("estimation1", estimation1);
        intent.putExtra("estimation2", estimation2);
        intent.putExtra("estimation3", estimation3);
        return intent;
    }

    public int[][] convertStringToArray(String input) {
        input = input.trim().replaceAll("\\ ", "");
        input = input+",";
        int[][] output = new int[4][4];
        input = input.replaceAll("\\{","");
        input = input.substring(0,input.length()-2) + ",";
        String[] elements = input.split("\\},");
        for(int i=0; i<elements.length; i++) {
            String element = elements[i] + ",";
            String[] elementRow = element.split(",");
            for(int j=0; j<elementRow.length; j++) {
                output[i][j] = Integer.parseInt(elementRow[j].trim() + "");
            }
        }
        return output;
    }

    public static int[][] getTranspose(int[][] A) {
        int[][] Atrans = new int[4][4];
        for (int i = 0; i < A.length; i++) {
            for (int j = 0; j < A[0].length; j++) {
                Atrans[i][j] = A[j][i];
            }
        }
        return Atrans;
    }

    public static String arrayToString(int[] A) {
        String arrayString = "{";
        for (int i = 0; i < A.length; i++) {
            arrayString += A[i] + ",";
        }
        arrayString = arrayString.substring(0, arrayString.length() - 1);
        arrayString += "}";
        return arrayString;
    }

    public static String arrayRCMultiply(String iV, String jV) {
        int sum = 0;
        for (int i = 0; i < iV.length(); i++) {
            if (iV.charAt(i) >= '0' && iV.charAt(i) <= '9') {
                sum += (((int) (iV.charAt(i)) - 48) * ((int) (jV.charAt(i)) - 48));
            }
        }
        return sum + "";
    }

    public static String ar2str(int[] arr) {
        String out = "";
        for(int i = 0; i<arr.length; i++) {
            out += arr[i] + "        \t";
        }
        return out;
    }
}
