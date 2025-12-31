package com.nisa.prolabproje3;

import java.util.ArrayList;
import java.util.List;

public class Article {
    private String id; 
    private String title;
    private int year;
    private String doi;
    
    // For Linked list - Doubly linked list
    public Article previous;
    public Article next;
  
    private List<String> authors = new ArrayList<>();
    private List<String> references = new ArrayList<>();
    private List<String> citingPaperIds = new ArrayList<>(); 
    private int citationCount = 0; 

    public Article() {
    }
    public void setId(String id){
        this.id = id; 
    }
    public void setTitle(String title){
        this.title = title; 
    }
    public void setYear(int year){
        this.year = year; 
    }
    public void setDoi(String doi){
        this.doi = doi; 
    }
    
    public void incrementCitationCount(){
        this.citationCount++;
    }
    
    public void setCitationCount(int count){
        this.citationCount = count;
    }
    
    public void addAuthor(String author){
        this.authors.add(author);
    }
    
    public void addReference(String refId){
        this.references.add(refId);
    }
    
    // The article keeps track of papers that cite it.
    public void addCitingPaper(String id){
        this.citingPaperIds.add(id);
    }
    public String getId(){
        return id; 
    }
    public String getTitle(){
        return title; 
    }
    public int getYear(){
        return year; 
    }
    public List<String> getAuthors(){
        return authors; 
    }
    public List<String> getReferences(){
        return references; 
    }
    
    public List<String> getCitingPaperIds(){
        return citingPaperIds; 
    }
    
    public int getCitationCount(){
        return citationCount; 
    }
    @Override
    public String toString(){
        return "ID: " + id + " | Year: " + year;
    }
}