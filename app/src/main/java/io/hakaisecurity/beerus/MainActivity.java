package io.hakaisecurity.beerus;

import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private FileListAdapter adapter;
    private String selectedItem;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<String> fileNames = listFilesInDataData();

        if (android.os.Build.VERSION.SDK_INT > 8)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }


        FileListAdapter.OnItemClickListener listener = new FileListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(String item) {
                // Toast.makeText(MainActivity.this, "Selected: " + item, Toast.LENGTH_SHORT).show();
                selectedItem = item;
            }

        };

        adapter = new FileListAdapter(fileNames, listener);
        recyclerView.setAdapter(adapter);

        SearchView searchView = findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                return false;
            }
        });
    }

    public void sendZip(View view) {
        TextView ipAddressView = findViewById(R.id.editIpAddress);
        TextView portNumberView = findViewById(R.id.editPort);

        String ipAddress = ipAddressView.getText().toString().trim();
        String portString = portNumberView.getText().toString().trim();

        if (!isValidIpAddress(ipAddress)) {
            Toast.makeText(this, "Invalid IP ADDRESS", Toast.LENGTH_LONG).show();
            return;
        }

        int portNumber;
        try {
            portNumber = Integer.parseInt(portString);
            if (portNumber < 0 || portNumber > 65535) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid PORT", Toast.LENGTH_LONG).show();
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                FileZipper.main(new String[]{selectedItem, ipAddress, portString});
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Package sent to VPS", Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();
    }

    private boolean isValidIpAddress(String ipAddress) {
        try {
            if (ipAddress == null || ipAddress.isEmpty()) {
                return false;
            }
            String[] parts = ipAddress.split("\\.");
            if (parts.length != 4) {
                return false;
            }
            for (String s : parts) {
                int i = Integer.parseInt(s);
                if ((i < 0) || (i > 255)) {
                    return false;
                }
            }
            if (ipAddress.endsWith(".")) {
                return false;
            }
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    private List<String> listFilesInDataData() {
        List<String> filesList = new ArrayList<>();
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream outputStream = new DataOutputStream(process.getOutputStream());
            outputStream.writeBytes("ls /data/data/\n");
            outputStream.writeBytes("exit\n");
            outputStream.flush();
            process.waitFor();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                filesList.add(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return filesList;
    }

    public static class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.ViewHolder> implements Filterable {

        private List<String> data;
        private List<String> filteredData;
        private OnItemClickListener listener;

        public int selectedPosition = -1; // Variable to track the selected position

        public interface OnItemClickListener {
            void onItemClick(String item);
        }

        public FileListAdapter(List<String> data, OnItemClickListener listener) {
            this.data = data;
            this.filteredData = new ArrayList<>(data); // Initialize filteredData with data
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_item_radio, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String item = filteredData.get(position);
            holder.textView.setText(item);

            // Set the radio button state based on the current selection
            holder.radioBtn.setChecked(position == selectedPosition);

            // Define a click listener for the itemView and radio button
            View.OnClickListener clickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Use holder.getAdapterPosition() instead of the position parameter
                    int adapterPosition = holder.getAdapterPosition();

                    // Check for NO_POSITION
                    if (adapterPosition == RecyclerView.NO_POSITION) return;

                    if (selectedPosition != adapterPosition) {
                        selectedPosition = adapterPosition;
                    } else {
                        selectedPosition = -1; // Deselect if the same item is clicked again
                    }
                    notifyDataSetChanged(); // Refresh the list to update radio button states

                    listener.onItemClick(filteredData.get(adapterPosition)); // Use the correct position to get the item
                }
            };

            // Set the same click listener to both itemView and radio button
            holder.itemView.setOnClickListener(clickListener);
            holder.radioBtn.setOnClickListener(clickListener);
        }

        @Override
        public int getItemCount() {
            return filteredData.size();
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    List<String> filteredResults = new ArrayList<>();
                    if (constraint == null || constraint.length() == 0) {
                        filteredResults.addAll(data);
                    } else {
                        String filterPattern = constraint.toString().toLowerCase().trim();
                        for (String item : data) {
                            if (item.toLowerCase().contains(filterPattern)) {
                                filteredResults.add(item);
                            }
                        }
                    }

                    FilterResults results = new FilterResults();
                    results.values = filteredResults;
                    return results;
                }

                @SuppressWarnings("unchecked")
                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    filteredData.clear();
                    filteredData.addAll((List<String>) results.values);
                    notifyDataSetChanged();
                    selectedPosition = -1; // Reset selected position on filter change
                }
            };
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;
            RadioButton radioBtn;

            ViewHolder(View itemView) {
                super(itemView);
                textView = itemView.findViewById(R.id.text_view); // Ensure this ID matches with your recycler_item_radio.xml
                radioBtn = itemView.findViewById(R.id.radio_button);
            }
        }
    }

}