package com.example.tradeup.View.FragmentNav;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.tradeup.Controller.ProductController;
import com.example.tradeup.Model.Product;
import com.example.tradeup.R;
import com.example.tradeup.View.Adapter.ImageSliderAdapter;
import com.example.tradeup.View.Adapter.ProductAdapter;
import com.example.tradeup.View.CartActivity;
import com.example.tradeup.View.ChitietActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private ImageView imgAvatar;
    private TextView tvUserName;
    private SearchView searchView;
    private RecyclerView recyclerViewProducts;
    private ProductAdapter adapter;
    private List<Product> productList;
    private List<Product> allProducts;
    private ViewPager2 viewPager;
    private Handler sliderHandler = new Handler();

    private final Runnable slideRunnable = () -> {
        int next = (viewPager.getCurrentItem() + 1) % 3;
        viewPager.setCurrentItem(next, true);
    };

    public HomeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        imgAvatar = view.findViewById(R.id.imgAvatar);
        tvUserName = view.findViewById(R.id.tvUserName);
        searchView = view.findViewById(R.id.searchView);
        viewPager = view.findViewById(R.id.viewPager);
        recyclerViewProducts = view.findViewById(R.id.recyclerViewProducts);

        loadUserProfile();
        setupSearchView();
        setupViewPager();
        setupRecyclerView();
        ImageButton btnCart = view.findViewById(R.id.btnCart);
        btnCart.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), CartActivity.class);
            startActivity(intent);
        });


        return view;
    }

    private void loadUserProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        String username = snapshot.getString("username");
                        String avatarUrl = snapshot.getString("avatar");

                        tvUserName.setText(username != null ? username : "Người dùng");

                        if (avatarUrl != null && !avatarUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(avatarUrl)
                                    .placeholder(R.drawable.default_avatar)
                                    .circleCrop()
                                    .into(imgAvatar);
                        } else {
                            imgAvatar.setImageResource(R.drawable.default_avatar);
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Không thể tải thông tin người dùng", Toast.LENGTH_SHORT).show());
    }

    private void setupSearchView() {
        searchView.setQueryHint("Tìm sản phẩm...");
        searchView.setIconified(true);

        EditText searchEditText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        searchEditText.setTextSize(14);
        searchEditText.setTextColor(ContextCompat.getColor(requireContext(), R.color.black)); // hoặc Color.BLACK
        searchEditText.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.gray));

        ImageView searchIcon = searchView.findViewById(androidx.appcompat.R.id.search_mag_icon);
        if (searchIcon != null) {
            searchIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.black));
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (!query.trim().isEmpty()) {
                    hideKeyboard();
                    searchFromFirestore(query);
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.trim().isEmpty()) {
                    productList.clear();
                    productList.addAll(allProducts); // Khôi phục danh sách gốc
                    adapter.notifyDataSetChanged();
                }
                return true;
            }
        });
    }

    private void hideKeyboard() {
        View view = getView();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void setupViewPager() {
        List<String> sliderImages = new ArrayList<>();
        sliderImages.add("https://i.pinimg.com/736x/89/7d/df/897ddfa0615c917d5f35473fb8ff6767.jpg");
        sliderImages.add("https://i.pinimg.com/736x/af/3b/c3/af3bc34b1a53501ab4a63ff6493cc6cd.jpg");
        sliderImages.add("https://i.pinimg.com/736x/29/92/9d/29929d406c5619efdac5cb2676ec270e.jpg");

        ImageSliderAdapter sliderAdapter = new ImageSliderAdapter(getContext(), sliderImages);
        viewPager.setAdapter(sliderAdapter);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                sliderHandler.removeCallbacks(slideRunnable);
                sliderHandler.postDelayed(slideRunnable, 3000);
            }
        });
    }

    private void setupRecyclerView() {
        recyclerViewProducts.setLayoutManager(new GridLayoutManager(getContext(), 2));
        productList = new ArrayList<>();
        allProducts = new ArrayList<>();

        adapter = new ProductAdapter(getContext(), productList, product -> {
            if (getContext() != null) {
                Intent intent = new Intent(getContext(), ChitietActivity.class);
                intent.putExtra("productId", product.getId());
                startActivity(intent);
            }
        });

        recyclerViewProducts.setAdapter(adapter);
        loadPopularProducts();
    }

    private void loadPopularProducts() {
        ProductController controller = new ProductController();
        controller.getAllProducts(task -> {
            if (task.isSuccessful()) {
                productList.clear();
                allProducts.clear();
                QuerySnapshot snapshot = task.getResult();
                if (snapshot != null) {
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Product product = doc.toObject(Product.class);
                        productList.add(product);
                        allProducts.add(product);
                    }
                    adapter.notifyDataSetChanged();
                }
            } else {
                Toast.makeText(getContext(), "Lỗi tải sản phẩm", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchFromFirestore(String query) {
        ProductController controller = new ProductController();
        controller.searchProductsByTitle(query, filteredProducts -> {
            productList.clear();
            productList.addAll(filteredProducts);
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        sliderHandler.removeCallbacks(slideRunnable);
    }
}
