package com.example.lv_map;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.daum.mf.map.api.MapPoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.CustomViewHolder> implements Filterable { //implements Filterable
    // adapter에 들어갈 list
    private ArrayList<ListData> mList = null;
    private ArrayList<ListData> filter = null;
    private Activity activity = null;
    static String testStr;

    private MainActivity mainActivity;

    private Context context = null;
    public static double longitude = 0.0, latitude = 0.0;
    static int breakNum = 0;

    private int num =0;
    //item 클릭 상태 저장
    private SparseBooleanArray selectedItems = new SparseBooleanArray();

    // 직전에 클릭됐던 Item의 position
    private int prePosition = -1;

    public ListAdapter(Activity activity, ArrayList<ListData> list) {
        this.activity = activity;
        this.mList = list;
        this.filter = list;
    }

    @Override
    public CustomViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        context = viewGroup.getContext();
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_info, null);
        CustomViewHolder viewHolder = new CustomViewHolder(view);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull CustomViewHolder viewholder, int position) {

        viewholder.onBind(mList.get(position), position);
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    class CustomViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        protected TextView name,detail_addr;
        protected TextView textType, textAddr, textNumber;
        protected LinearLayout linearLayout, showAll;

        public String textDetailaddr = "";
        private ListData listData=null;
        private int position;

        private Geocoder geocoder;


        CustomViewHolder(View view) {
            super(view);

            this.name = (TextView) view.findViewById(R.id.local_name);
            this.detail_addr = (TextView) view.findViewById(R.id.local_addr);
            this.linearLayout = (LinearLayout)view.findViewById(R.id.linearItem);

            this.showAll = (LinearLayout) view.findViewById(R.id.showAll);
            this.textAddr = (TextView)view.findViewById(R.id.textAddr);
            this.textNumber = (TextView)view.findViewById(R.id.textNumber);
            this.textType = (TextView)view.findViewById(R.id.textType);


            geocoder = new Geocoder(context);

        }

        void onBind(ListData data, int position) {
            this.listData = data;
            this.position = position;


            name.setText(data.getName());
            detail_addr.setText(data.getDetailaddr());

            textAddr.setText("행정동 : "+data.getAddr());
            textDetailaddr = data.getDetailaddr();
            textNumber.setText("번호 : "+data.getNumber());
            textType.setText("업종 : "+data.getType());

            changeVisibility(selectedItems.get(position));

            itemView.setOnClickListener(this);
            linearLayout.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            testStr = textDetailaddr;
            breakNum = 2; // 특정 입력에 대한 마커 처리 멈추기
            List<Address> list = null; //주소 저장을 위한 리스트
            MainActivity.myMap.removeAllPOIItems(); //맵뷰에 있던 모든 마커 정보 지우기

            if( testStr== null){
               Toast.makeText(context, "위치 지정할 곳을 선택해 주세요.", Toast.LENGTH_LONG).show();
            }
            else{
                try {
                    //String 값(주소)을 넣어 위도 경도 리스트에 추가
                    list = geocoder.getFromLocationName( testStr,10); // 얻어올 값의 개수
                } catch (IOException e) {
                    e.printStackTrace(); //주소가 없을시 오류
                    Log.e("test", "입출력 오류 - 서버에서 주소변환시 에러발생");
                }

                if (list != null) {
                    if (list.size() == 0) {
                        Toast.makeText(context, "해당되는 주소 정보는 없습니다.", Toast.LENGTH_LONG).show();
                    } else {
                        Address addr = list.get(0);
                        latitude = addr.getLatitude(); // 위도
                        longitude = addr.getLongitude();// 경도
                    }
                }

                //맵 포인트 좌표 생성
                MapPoint mapPoint = MapPoint.mapPointWithCONGCoord(latitude,longitude);
                MainActivity.myMap.setMapCenterPoint(mapPoint,true);
                //마커 생성시 마커로 화면중심 이동, 애니메이션 효과 true
                MainActivity.myMap.removeAllPOIItems(); //맵뷰에 있던 모든 마커 정보 지우기
                mainActivity = new MainActivity();
                //지도 화면에 마커 표시
                mainActivity.setDaumMapCurrentMarker(latitude,longitude,
                        name.getText()+"-"+textType.getText());

            }

            switch (v.getId()) {
                case R.id.linearItem:
                    if (selectedItems.get(position)) {
                        // 펼쳐진 Item을 클릭 시
                        selectedItems.delete(position);
                    } else {
                        // 직전의 클릭됐던 Item의 클릭상태를 지움
                        selectedItems.delete(prePosition);
                        // 클릭한 Item의 position을 저장
                        selectedItems.put(position, true);
                    }
                    // 해당 포지션의 변화를 알림
                    if (prePosition != -1) notifyItemChanged(prePosition);
                    notifyItemChanged(position);
                    // 클릭된 position 저장
                    prePosition = position;
                    break;
            }
            testStr = "";
        }

        private void changeVisibility(final boolean isExpanded) {
            //레이아웃 높이 값을 dp로 지정해서 아래 소스를 이용하여 넣음
            int dpValue = 90;
            float d = context.getResources().getDisplayMetrics().density;
            int height = (int) (dpValue * d);

            // ValueAnimator.ofInt(int... values)는 View가 변할 값을 지정, 인자 = int 배열
            ValueAnimator va = isExpanded ? ValueAnimator.ofInt(0, height)
                    : ValueAnimator.ofInt(height, 0);
            // 레이아웃 애니메이션이 실행되는 시간을 0.5초로 지정
            va.setDuration(500);
            //뷰가 바뀔때 모양 업데이트
            va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    // 높이
                    int height = (int) animation.getAnimatedValue();
                    // 리스트 아이템 클릭시 클릭된 리스트 정보 높이 변경
                    showAll.getLayoutParams().height = height;
                    showAll.requestLayout();
                    // 리스트의 자세한 정보가 보이는 뷰를
                    // isExpanded로 펼칠지 여부 확인
                    // 펼치면 보이게하고 안펼치면 사라지게함
                    showAll.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
                }
            });
            va.start();//펼치기 접히기 애니메이션 작동
        }
    }

    public void ClickListener(String strAddr,String name, String type){

        List<Address> list = null;
        double lat = 0;
        double lon = 0;
        num++;
        Geocoder geocoder = new Geocoder(context);
        try {
            list = geocoder.getFromLocationName(strAddr,10); // 얻어올 값의 개수
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("test", "입출력 오류 - 서버에서 주소변환시 에러발생");
        }

        if (list != null) {
            if (list.size() == 0) {
                    Toast.makeText(context, "해당되는 주소 정보는 없습니다.", Toast.LENGTH_LONG).show();
            } else {
                Address addr = list.get(0);
                lat = addr.getLatitude();
                lon = addr.getLongitude();
            }
        }

        MapPoint mapPoint = MapPoint.mapPointWithCONGCoord(lat,lon);
        if(num<2)MainActivity.myMap.setMapCenterPoint(mapPoint,true);
        if(num<2)MainActivity.myMap.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(lat, lon), true);
        mainActivity = new MainActivity();
        mainActivity.setDaumMapCurrentMarker(lat,lon,name+"-"+type);//지도 화면에 띄울 곳 지정
    }

    //RecyclerView의 필터 기능(입력한 값에 대한 결과만 보이기 위함)
    @Override
    public Filter getFilter() {
        return new Filter() {
            ArrayList<ListData> filterList = new ArrayList<>();
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                //constraint = EditText로 입력받은 문자열
                String charString = constraint.toString();

                //breakNum은 마커 찍히는거 제어하는 int형 변수
                //마커가 찍히는 동안 다른 이벤트가 있을시 원래있던거 지움
                if(breakNum==0)MainActivity.myMap.removeAllPOIItems();

                if(charString.isEmpty()) {//입력 받은 문자가 없을시
                    mList = filter; //리스트는 변경 없음
                    breakNum = 2; //마커 찍지 않음
                }
                else {
                    filterList = new ArrayList<>();
                    breakNum = 2;
                    for(ListData list : filter) { //행정동 검색시 리스트엔 검색된 행정동인 정보만 표시
                         if(MainActivity.string.equals("addr") && list.getAddr().equals(charString)) {
                            filterList.add(list);
                        }
                        else if(MainActivity.string.equals("name") && list.getName().equals(charString)) {
                            filterList.add(list);//상호명 검색시 리스트엔 검색된 상호명인 정보만 표시
                        }
                        else if(MainActivity.string.equals("type") && list.getType().equals(charString)) {
                             filterList.add(list);//업종명 검색시 리스트엔 검색된 업종명인 정보만 표시
                         }
                    }
                    mList = filterList; //필터링된 리스트

                    if(breakNum==0) breakNum = 0;
                    else breakNum =1;

                    for(ListData i: filterList){
                        if(breakNum<2){ //필터링되어 나타난 정보 모두 마커 표시
                            ClickListener(i.getDetailaddr(),i.getName(), i.getType());
                        }
                        else break;
                    }
                }

                //필터링된 결과 사용자에게 return
                FilterResults filterResults = new FilterResults();
                filterResults.values = mList;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                mList = (ArrayList<ListData>)results.values;//필터링 결과 리스트
                notifyDataSetChanged(); //새로고침으로 리스트뷰 필터링결과로 바꿔줌
            }
        };
    }
}
