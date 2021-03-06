package me.xiaopan.sketchsample.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;

import org.apache.http.HttpResponse;

import java.util.ArrayList;
import java.util.List;

import me.xiaopan.androidinjector.InjectContentView;
import me.xiaopan.androidinjector.InjectView;
import me.xiaopan.assemblyadapter.AssemblyRecyclerAdapter;
import me.xiaopan.gohttp.GoHttp;
import me.xiaopan.gohttp.HttpRequest;
import me.xiaopan.gohttp.HttpRequestFuture;
import me.xiaopan.gohttp.StringHttpResponseHandler;
import me.xiaopan.prl.PullRefreshLayout;
import me.xiaopan.sketchsample.MyFragment;
import me.xiaopan.sketchsample.R;
import me.xiaopan.sketchsample.activity.ApplyBackgroundCallback;
import me.xiaopan.sketchsample.activity.StarHomeActivity;
import me.xiaopan.sketchsample.adapter.itemfactory.HotStarThreeLeftItemFactory;
import me.xiaopan.sketchsample.adapter.itemfactory.HotStarThreeRightItemFactory;
import me.xiaopan.sketchsample.adapter.itemfactory.HotStarTwoItemFactory;
import me.xiaopan.sketchsample.adapter.itemfactory.ItemTitleItemFactory;
import me.xiaopan.sketchsample.bean.ThreeStarLeft;
import me.xiaopan.sketchsample.bean.ThreeStarRight;
import me.xiaopan.sketchsample.bean.TwoStar;
import me.xiaopan.sketchsample.net.request.HotManStarRequest;
import me.xiaopan.sketchsample.net.request.HotStarRequest;
import me.xiaopan.sketchsample.net.request.HotWomanStarRequest;
import me.xiaopan.sketchsample.util.ScrollingPauseLoadManager;
import me.xiaopan.sketchsample.widget.HintView;

/**
 * 热门明星页
 */
@InjectContentView(R.layout.fragment_hot_star)
public class HotStarFragment extends MyFragment implements PullRefreshLayout.OnRefreshListener, HotStarThreeLeftItemFactory.OnStarClickListener {

    @InjectView(R.id.refreshLayout_hotStar) private PullRefreshLayout refreshLayout;
    @InjectView(R.id.hint_hotStar) private HintView hintView;
    @InjectView(R.id.recyclerView_hotStar_content) private RecyclerView contentRecyclerView;

    private HttpRequestFuture httpRequestFuture;
    private AssemblyRecyclerAdapter adapter;

    private ApplyBackgroundCallback applyBackgroundCallback;
    private String backgroundImageUri;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof ApplyBackgroundCallback) {
            applyBackgroundCallback = (ApplyBackgroundCallback) activity;
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        contentRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        contentRecyclerView.setOnScrollListener(new ScrollingPauseLoadManager(view.getContext()));
        refreshLayout.setOnRefreshListener(this);

        if (adapter == null) {
            refreshLayout.startRefresh();
        } else {
            contentRecyclerView.setAdapter(adapter);
            contentRecyclerView.scheduleLayoutAnimation();
        }
    }

    @Override
    public void onRefresh() {
        if (httpRequestFuture != null && !httpRequestFuture.isFinished()) {
            return;
        }

        load(false, false, false);
    }

    @Override
    public void onDetach() {
        if (httpRequestFuture != null && !httpRequestFuture.isFinished()) {
            httpRequestFuture.cancel(true);
        }
        super.onDetach();
    }

    @Override
    protected void onUserVisibleChanged(boolean isVisibleToUser) {
        if (applyBackgroundCallback != null && isVisibleToUser) {
            changeBackground(backgroundImageUri);
        }
    }

    @Override
    public void onClickImage(HotStarRequest.Star star) {
        StarHomeActivity.launch(getActivity(), star.getName());
    }

    private void changeBackground(String imageUri) {
        this.backgroundImageUri = imageUri;
        if (applyBackgroundCallback != null) {
            applyBackgroundCallback.onApplyBackground(backgroundImageUri);
        }
    }

    private void load(boolean isMan, final boolean last, final boolean haveSetWindowBackground) {
        httpRequestFuture = GoHttp.with(getActivity()).newRequest(isMan ? new HotManStarRequest() : new HotWomanStarRequest(), new StringHttpResponseHandler(), new HttpRequest.Listener<List<HotStarRequest.HotStar>>() {
            @Override
            public void onStarted(HttpRequest httpRequest) {
                hintView.hidden();
            }

            @Override
            public void onCompleted(HttpRequest httpRequest, HttpResponse httpResponse, List<HotStarRequest.HotStar> hotStarList, boolean b, boolean b2) {
                if (last) {
                    appendData(adapter, hotStarList);
                    contentRecyclerView.setAdapter(adapter);
                    contentRecyclerView.scheduleLayoutAnimation();
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            refreshLayout.stopRefresh();
                        }
                    }, 1000);
                    if (!haveSetWindowBackground && hotStarList.size() > 0 && hotStarList.get(0).getStarList().size() > 0) {
                        changeBackground(hotStarList.get(0).getStarList().get(0).getHeightImage().getUrl());
                    }
                } else {
                    adapter = new AssemblyRecyclerAdapter(new ArrayList<Object>());
                    appendData(adapter, hotStarList);
                    adapter.addItemFactory(new HotStarThreeLeftItemFactory(HotStarFragment.this));
                    adapter.addItemFactory(new HotStarThreeRightItemFactory(HotStarFragment.this));
                    adapter.addItemFactory(new HotStarTwoItemFactory(HotStarFragment.this));
                    adapter.addItemFactory(new ItemTitleItemFactory());
                    boolean result = false;
                    if (hotStarList.size() > 0 && hotStarList.get(0).getStarList().size() > 0) {
                        changeBackground(hotStarList.get(0).getStarList().get(0).getHeightImage().getUrl());
                        result = true;
                    }
                    load(true, true, result);
                }
            }

            @Override
            public void onFailed(HttpRequest httpRequest, HttpResponse httpResponse, HttpRequest.
                    Failure failure, boolean b, boolean b2) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        refreshLayout.stopRefresh();
                    }
                }, 1000);
                if (adapter == null) {
                    hintView.failed(failure, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            refreshLayout.startRefresh();
                        }
                    });
                } else {
                    Toast.makeText(getActivity(), "刷新失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCanceled(HttpRequest httpRequest) {

            }

            public void appendData(AssemblyRecyclerAdapter adapter, List<HotStarRequest.HotStar> hotStarList) {
                for (HotStarRequest.HotStar hotStar : hotStarList) {
                    adapter.getDataList().add(hotStar.getName());
                    parse(adapter, hotStar.getStarList());
                }
            }

            private void parse(AssemblyRecyclerAdapter adapter, List<HotStarRequest.Star> starList) {
                if (starList == null) {
                    return;
                }
                boolean left = true;
                for (int w = 0, size = starList.size(); w < size; ) {
                    int number = size - w;
                    if (number == 1) {
                        TwoStar oneItem = new TwoStar();
                        oneItem.star1 = starList.get(w++);
                        adapter.getDataList().add(oneItem);
                    } else if (number == 2) {
                        TwoStar twoStar = new TwoStar();
                        twoStar.star1 = starList.get(w++);
                        twoStar.star2 = starList.get(w++);
                        adapter.getDataList().add(twoStar);
                    } else {
                        if (left) {
                            ThreeStarLeft threeStarLeft = new ThreeStarLeft();
                            threeStarLeft.star1 = starList.get(w++);
                            threeStarLeft.star2 = starList.get(w++);
                            threeStarLeft.star3 = starList.get(w++);
                            adapter.getDataList().add(threeStarLeft);
                        } else {
                            ThreeStarRight threeStarRight = new ThreeStarRight();
                            threeStarRight.star1 = starList.get(w++);
                            threeStarRight.star2 = starList.get(w++);
                            threeStarRight.star3 = starList.get(w++);
                            adapter.getDataList().add(threeStarRight);
                        }
                        left = !left;
                    }
                }
            }
        }).responseHandleCompletedAfterListener(new HotStarRequest.ResponseHandler(isMan)).go();
    }
}