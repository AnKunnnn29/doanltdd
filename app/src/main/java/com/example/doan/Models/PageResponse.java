package com.example.doan.Models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PageResponse<T> {
    
    @SerializedName("content")
    private List<T> content;
    
    @SerializedName("totalElements")
    private long totalElements;
    
    @SerializedName("totalPages")
    private int totalPages;
    
    @SerializedName("size")
    private int size;
    
    @SerializedName("number")
    private int number;
    
    @SerializedName("first")
    private boolean first;
    
    @SerializedName("last")
    private boolean last;
    
    @SerializedName("empty")
    private boolean empty;
    
    public PageResponse() {}
    
    public List<T> getContent() {
        return content;
    }
    
    public void setContent(List<T> content) {
        this.content = content;
    }
    
    public long getTotalElements() {
        return totalElements;
    }
    
    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }
    
    public int getTotalPages() {
        return totalPages;
    }
    
    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
    
    public int getSize() {
        return size;
    }
    
    public void setSize(int size) {
        this.size = size;
    }
    
    public int getNumber() {
        return number;
    }
    
    public void setNumber(int number) {
        this.number = number;
    }
    
    public boolean isFirst() {
        return first;
    }
    
    public void setFirst(boolean first) {
        this.first = first;
    }
    
    public boolean isLast() {
        return last;
    }
    
    public void setLast(boolean last) {
        this.last = last;
    }
    
    public boolean isEmpty() {
        return empty;
    }
    
    public void setEmpty(boolean empty) {
        this.empty = empty;
    }
}
