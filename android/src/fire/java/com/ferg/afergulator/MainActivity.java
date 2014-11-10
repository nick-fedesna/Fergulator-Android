package com.ferg.afergulator;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.util.SimpleArrayMap;
import android.view.*;
import android.widget.*;

import java.util.*;

import butterknife.ButterKnife;
import com.sample.amazon.asbuilibrary.list.CarouselView;
import com.sample.amazon.asbuilibrary.list.adapter.BasePagingCarouselAdapter;

public class MainActivity extends Activity {

    private SimpleArrayMap<Integer, String> mGameMap;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        // get ROMS
        mGameMap = new SimpleArrayMap<Integer, String>();
        mGameMap.put(0, "Super Mario Bros.nes");

        List<Integer> ids = new ArrayList<Integer>();
        ids.add(0);

        PagingBoxesCarouselAdapter adapter = new PagingBoxesCarouselAdapter(ids);


        // Set our adapter on our carousel
        @SuppressWarnings("unchecked")
        CarouselView<BasePagingCarouselAdapter<Integer, Object>> boxesCarousel = (CarouselView<BasePagingCarouselAdapter<Integer, Object>>) findViewById(
                com.sample.amazon.uiwidgetssample.R.id.boxes_carousel);
        boxesCarousel.setAdapter(adapter);
        adapter.attachToCarousel(boxesCarousel);

        boxesCarousel.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String rom = mGameMap.get(position);
                Intent game = new Intent(MainActivity.this, GameActivity.class);
                game.putExtra("rom", rom);
                startActivity(game);
            }
        });


        Intent game = new Intent(MainActivity.this, GameActivity.class);
        game.putExtra("rom", "Super Mario Bros.nes");
        startActivity(game);
    }


    /**
     * Adapter for our Carousel to show simple boxes with text, and toast when items are requested
     */
    private class PagingBoxesCarouselAdapter extends BasePagingCarouselAdapter<Integer, Object>
    {
        public PagingBoxesCarouselAdapter(List<Integer> ids)
        {
            super(ids);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            // Does our view exist?
            if (convertView == null)
            {
                convertView = LayoutInflater.from(MainActivity.this).inflate(
                        com.sample.amazon.uiwidgetssample.R.layout.carousel_item,
                        parent, false);
            }

            // Set the text
            TextView text = (TextView) convertView.findViewById(com.sample.amazon.uiwidgetssample.R.id.text);
            text.setText(Integer.toString(position));
            text.setText(mGameMap.get(position));

            return convertView;
        }

        @Override
        protected void requestItemData(List<Integer> listOfIdsToRequest)
        {
            if (!listOfIdsToRequest.isEmpty())
            {
                // First sort the data
                Collections.sort(listOfIdsToRequest);

                // Show the largest and smallest item requested
//                Toast.makeText(
//                        MainActivity.this,
//                        "IDs: " + listOfIdsToRequest.get(0) + " - "
//                                + listOfIdsToRequest.get(listOfIdsToRequest.size() - 1) + " requested!",
//                        Toast.LENGTH_LONG).show();

                // You can put the retrieved data in mItemMap and it will be returned on a call to getItem(int)
            }
        }
    }
}
