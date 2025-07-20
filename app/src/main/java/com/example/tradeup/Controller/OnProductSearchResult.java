package com.example.tradeup.Controller;

import com.example.tradeup.Model.Product;
import java.util.List;
public interface OnProductSearchResult {
    void onResult(List<Product> products);
}
