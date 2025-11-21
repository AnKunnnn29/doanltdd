package com.example.doan;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class StoreFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_store, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.stores_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        List<Store> storeList = new ArrayList<>();
        storeList.add(new Store("The Coffee House - Su Van Hanh", "798 Su Van Hanh, District 10, HCMC", R.drawable.banners));
        storeList.add(new Store("The Coffee House - CMT8", "78 Cach Mang Thang 8, District 3, HCMC", R.drawable.banners));
        storeList.add(new Store("The Coffee House - Nguyen Thi Minh Khai", "199 Nguyen Thi Minh Khai, District 1, HCMC", R.drawable.banners));

        StoreAdapter adapter = new StoreAdapter(storeList);
        recyclerView.setAdapter(adapter);

        return view;
    }
}
