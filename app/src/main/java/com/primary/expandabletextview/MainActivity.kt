package com.primary.expandabletextview

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var expandableTextView: ExpandableTextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        expandableTextView = findViewById(R.id.expandableTextView)
        expandableTextView.initWidth(getWindowManager().getDefaultDisplay().getWidth())
        expandableTextView.maxLines = 2
        expandableTextView.setCloseInNewLine(true)
        expandableTextView.setTitleColor(Color.RED)
        expandableTextView.setTitleSuffix("请注意")
        expandableTextView.setOriginalText(
            "请注意  在全球，随着Flutter被越来越多的知名公司应用在自己的商业APP中，" +
                    "Flutter这门新技术也逐渐进入了移动开发者的视野，尤其是当Google在2018年IO大会上发布了第一个" +
                    "Preview版本后，国内刮起来一股学习Flutter的热潮。\n\n为了更好的方便帮助中国开发者了解这门新技术" +
                    "，我们，Flutter中文网，前后发起了Flutter翻译计划、Flutter开源计划，前者主要的任务是翻译" +
                    "Flutter官方文档，后者则主要是开发一些常用的包来丰富Flutter生态，帮助开发者提高开发效率。而时" +
                    "至今日，这两件事取得的效果还都不错！"
        )
    }
}