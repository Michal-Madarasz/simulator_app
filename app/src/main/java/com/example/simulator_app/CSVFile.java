package com.example.simulator_app;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class CSVFile {
    InputStream inputStream;
    OutputStream outputStream;

    public CSVFile(InputStream inputStream, OutputStream outputStream){
        setCSVFile(inputStream, outputStream);
    }

    public void setCSVFile(InputStream inputStream, OutputStream outputStream){
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    public ArrayList<String[]> read(){
        ArrayList<String[]> resultList = new ArrayList();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            String csvLine;
            while ((csvLine = reader.readLine()) != null) {
                String[] row = csvLine.split(",");
                resultList.add(row);
            }
        }
        catch (IOException ex) {
            throw new RuntimeException("Error in reading CSV file: "+ex);
        }
        finally {
            try {
                inputStream.close();
            }
            catch (IOException e) {
                throw new RuntimeException("Error while closing input stream: "+e);
            }
        }
        return resultList;
    }

    public void write(ArrayList<String> rows)
    {
        try {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(outputStream));
            for(String csvLine : rows)
            {
                bw.write(csvLine);      // zalozenie ze zapisujemy obiekt z przecinkami pomiedzy wartosciami zmiennych
                bw.newLine();
            }
        }catch(IOException e) {

        }finally {
            try {
                outputStream.close();
            }
            catch (IOException e) {
                throw new RuntimeException("Error while closing output stream: "+e);
            }
        }
    }
}