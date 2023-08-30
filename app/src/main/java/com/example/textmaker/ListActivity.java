package com.example.textmaker;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.textmaker.databinding.TextListActivityBinding;

public class ListActivity extends AppCompatActivity implements ListAdapter.onNoteClickListener
{
    private TextListActivityBinding binding;
    private ListAdapter listAdapter;
    private final MainAppData data = new MainAppData(this);

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        binding = TextListActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        createListAdapter();
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        binding.clear.setOnClickListener(view -> {
            listAdapter.itemsRemoved();
            Toast.makeText(this,
                    R.string.items_removed,
                    Toast.LENGTH_SHORT).show();
        });
        binding.goBack.setOnClickListener(view -> onBackPressed());
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        data.saveListData();
    }

    @Override
    public void onNoteClick(int position) {
        Intent intent = new Intent(this, TextWorkSpaceActivity.class);
        intent.putExtra(MainAppData.textToEditTag, MainAppData.dataList.get(position));
        startActivity(intent);
        finish();
    }

    private void createListAdapter()
    {
        binding.textRecyclerView.setHasFixedSize(true);
        binding.textRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        listAdapter = new ListAdapter(this, MainAppData.dataList,this);
        binding.textRecyclerView.setAdapter(listAdapter);
    }
}
