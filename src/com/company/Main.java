package com.company;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;


public class Main {

    static double extractFirstPage(String[] splittedText, PrintWriter log, PrintWriter out, int[] finalIndexOutput) {

        float sum=0;
        int conditionLine=0;

        log.println("Searching for starting string condition line by line...");

        for (int i=0; i<splittedText.length; i++) {
            if (splittedText[i].contains("Transactions for Card:")) {
                conditionLine = i;
                log.println("Starting string condition found at line " + "#" + (conditionLine+1) + ". Beginning extraction of relevant data.");
                break;
            }
        }

        for (int i=conditionLine+1; i<splittedText.length; i++) {
            if (!splittedText[i].contains("Cover Amount")) {
                String[] splittedLine = splittedText[i].split("\\s+");  //splits each line into words using whitespace "\\s+"
                log.print(splittedLine[1] + " | ");
                out.print(splittedLine[1] + " | ");                   //prints transaction date

                String[] copiedTransactionDetails = new String[splittedLine.length - 3];
                for (int j = 3, k = 0; j < splittedLine.length; j++, k++) {
                    copiedTransactionDetails[k] = splittedLine[j];
                }
                String finalTransactionDetails = String.join(" ", copiedTransactionDetails);

                String[] splittedAmountString = splittedLine[2].split("(?=\\D)(?<=\\d)");
                if (!splittedLine[2].contains(",")) {
                    String amount = splittedAmountString[0] + splittedAmountString[1];
                    sum += Double.parseDouble(amount);
                    log.print(splittedAmountString[2] + " " + finalTransactionDetails + " | " + amount);
                    out.print(splittedAmountString[2] + " " + finalTransactionDetails + " | " + amount);
                }
                else {
                    String amount = splittedAmountString[0] + splittedAmountString[1] + splittedAmountString[2];
                    amount = amount.replace(",", "");
                    sum += Double.parseDouble(amount);
                    log.print(splittedAmountString[3] + " " + finalTransactionDetails + " | " + amount);
                    out.print(splittedAmountString[3] + " " + finalTransactionDetails + " | " + amount);
                }
                log.println();
                out.println();
            }
            else {
                log.println("Finished extraction from page " + 1 + ". Moving to page " + 2 + ".");
                finalIndexOutput[0]=i;
                break;
            }
        }
        return sum;
    }

    static double extractOtherPages(String[] splittedText, PrintWriter log, PrintWriter out, int startIndexReference, int pageNumber, int[] finalIndexOutput, boolean[] end) {

        double sum = 0;
        int conditionLine = 0;

        log.println("Searching for starting string condition line by line...");

        for (int i=startIndexReference+1; i<splittedText.length; i++) {
            if (splittedText[i].contains("Trxn Date")) {
                conditionLine = i;
                log.println("Starting string condition found at line " + "#" + (conditionLine+1) + ". Beginning extraction of relevant data.");
                break;
            }
        }

        for (int i=conditionLine+1; i<splittedText.length; i++) {

            if (splittedText[i].contains("Previous Points")) {
                end[0] = true;
                break;
            }

            if (splittedText[i].contains("Murabaha") || splittedText[i].endsWith("CR")) {
                continue;
            }

            if (!splittedText[i].contains("Cover Amount")) {
                String[] splittedLine = splittedText[i].split("\\s+");  //splits each line into words using whitespace "\\s+"
                log.print(splittedLine[1] + " | ");
                out.print(splittedLine[1] + " | ");                   //prints transaction date

                String[] copiedTransactionDetails = new String[splittedLine.length - 3];
                for (int j = 3, k = 0; j < splittedLine.length; j++, k++) {
                    copiedTransactionDetails[k] = splittedLine[j];
                }
                String finalTransactionDetails = String.join(" ", copiedTransactionDetails);

                String[] splittedAmountString = splittedLine[2].split("(?=\\D)(?<=\\d)");
                if (!splittedLine[2].contains(",")) {
                    String amount = splittedAmountString[0] + splittedAmountString[1];
                    sum += Double.parseDouble(amount);
                    log.print(splittedAmountString[2] + " " + finalTransactionDetails + " | " + amount);
                    out.print(splittedAmountString[2] + " " + finalTransactionDetails + " | " + amount);
                }
                else {
                    String amount = splittedAmountString[0] + splittedAmountString[1] + splittedAmountString[2];
                    amount = amount.replace(",", "");
                    sum += Double.parseDouble(amount);
                    log.print(splittedAmountString[3] + " " + finalTransactionDetails + " | " + amount);
                    out.print(splittedAmountString[3] + " " + finalTransactionDetails + " | " + amount);
                }
                log.println();
                out.println();
            }
            else {
                log.println("Finished extraction from page " + pageNumber + ". Moving to page " + (pageNumber + 1) + ".");
                finalIndexOutput[pageNumber-1] = i;
                break;
            }
        }

        return sum;
    }

    static boolean checkBalance(String[] splittedText, double calculatedBalance) {

        int correctBalance;
        boolean atBalanceLine = false;

        for (int i=0; i<splittedText.length; i++) {
            if (splittedText[i].contains("Points Redeemed")) {
                atBalanceLine = true;
                continue;
            }

            if (atBalanceLine) {
                String[] splittedLine = splittedText[i].split("\\s+");
                splittedLine[2] = splittedLine[2].replace("=", "");
                correctBalance = Integer.parseInt(splittedLine[2]);

                int calculatedBalanceInt = (int) Math.rint(calculatedBalance);
                //int correctBalanceInt = (int) correctBalance;

                if (calculatedBalanceInt == correctBalance) {
                    return true;
                }

                atBalanceLine = false;
            }
        }

        return false;
    }



    public static void main(String[] args) throws IOException {
        File file = new File("C:\\Users\\Muaath\\Desktop\\New folder\\bank.pdf");
        PDDocument document = PDDocument.load(file, "44219693");
        document.setAllSecurityToBeRemoved(true);

        PrintWriter log = new PrintWriter("C:\\Users\\Muaath\\Desktop\\New folder\\log.txt");
        PrintWriter out = new PrintWriter("C:\\Users\\Muaath\\Desktop\\New folder\\text.txt");

        int totalPages = document.getNumberOfPages();
        double sum=0;

        PDFTextStripper pdfStripper = new PDFTextStripper();
        String text = pdfStripper.getText(document);

        String[] splittedText = text.split("\\r?\\n");

        int[] index = new int[1];

        sum += extractFirstPage(splittedText, log, out, index);

        int[] newIndex = new int[totalPages];
        newIndex[0] = index[0];
        boolean[] end = new boolean[1];

        for (int i=0; i<totalPages-1; i++) {
            if(!end[0]) {
                sum += extractOtherPages(splittedText, log, out, newIndex[i], (i + 2), newIndex, end);
            }
        }

        double roundOff = Math.round(sum * 100.0) / 100.0;
        log.println(roundOff);

        if (checkBalance(splittedText, roundOff)) {
            log.println("Values verified. Calculated balance is equal to correct balance in the document. Everything should be working well!");
        }
        else {
            log.println("Calculated Balance does not equal the correct balance in the document. Something is wrong!");
        }

        document.close();
        out.close();
        log.close();
    }
}
