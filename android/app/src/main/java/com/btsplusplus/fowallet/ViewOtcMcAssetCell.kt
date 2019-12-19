package com.btsplusplus.fowallet

import android.app.Activity
import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import bitshares.dp
import bitshares.forEach
import org.json.JSONObject

class ViewOtcMcAssetCell : LinearLayout {

    var _ctx: Context
    var _data: JSONObject

    private val content_fontsize = 12.0f

    constructor(ctx: Context, data: JSONObject) : super(ctx) {
        _ctx = ctx
        _data = data
        createUI()
    }

    private fun createUI(): LinearLayout {
        val layout_params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 24.dp)
        layout_params.gravity = Gravity.CENTER_VERTICAL

        val layout_wrap = LinearLayout(_ctx)
        layout_wrap.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        layout_wrap.orientation = LinearLayout.VERTICAL
        layout_wrap.setPadding(0,0,0,10.dp)

        // 第一行 商家图标 商家名称 交易总数|成交比
        val ly1 = LinearLayout(_ctx).apply {
            layoutParams = layout_params
            orientation = LinearLayout.HORIZONTAL

            // 第一行 资产名称
            addView(LinearLayout(_ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0.dp, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                gravity = Gravity.CENTER_VERTICAL or Gravity.LEFT

                addView(TextView(_ctx).apply {
                    text = _data.getString("asset_name").toString()
                    setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14.0f)
                    setTextColor(_ctx.resources.getColor(R.color.theme01_textColorMain))
                })
            })
        }

        // 第二行 可用 冻结 平台手续费(文本)
        val ly2 = LinearLayout(_ctx).apply {
            layoutParams = layout_params
            orientation = LinearLayout.HORIZONTAL

            // 左边
            addView(LinearLayout(_ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0.dp, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                gravity = Gravity.CENTER_VERTICAL or Gravity.LEFT

                addView(TextView(_ctx).apply {
                    text = "可用"
                    setTextSize(TypedValue.COMPLEX_UNIT_DIP, content_fontsize)
                    setTextColor(_ctx.resources.getColor(R.color.theme01_textColorGray))
                })
            })
            // 中间
            addView(LinearLayout(_ctx).apply {
                val _layout_params = LinearLayout.LayoutParams(0.dp, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                layoutParams = _layout_params
                gravity = Gravity.CENTER_VERTICAL or Gravity.CENTER

                addView(TextView(_ctx).apply {
                    text = "冻结"
                    setTextSize(TypedValue.COMPLEX_UNIT_DIP, content_fontsize)
                    setTextColor(_ctx.resources.getColor(R.color.theme01_textColorGray))
                })
            })
            // 右边
            addView(LinearLayout(_ctx).apply {
                val _layout_params = LinearLayout.LayoutParams(0.dp, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                layoutParams = _layout_params
                gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT

                addView(TextView(_ctx).apply {
                    text = "平台手续费"
                    setTextSize(TypedValue.COMPLEX_UNIT_DIP, content_fontsize)
                    setTextColor(_ctx.resources.getColor(R.color.theme01_textColorGray))
                })
            })
        }

        // 第三行 可用 冻结 平台手续费(值)
        val ly3 = LinearLayout(_ctx).apply {
            layoutParams = layout_params
            orientation = LinearLayout.HORIZONTAL

            // 左边
            addView(LinearLayout(_ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0.dp, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                gravity = Gravity.CENTER_VERTICAL or Gravity.LEFT

                addView(TextView(_ctx).apply {
                    text = _data.getString("available")
                    setTextSize(TypedValue.COMPLEX_UNIT_DIP, content_fontsize)
                    setTextColor(_ctx.resources.getColor(R.color.theme01_textColorNormal))
                })
            })
            // 中间
            addView(LinearLayout(_ctx).apply {
                val _layout_params = LinearLayout.LayoutParams(0.dp, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                layoutParams = _layout_params
                gravity = Gravity.CENTER_VERTICAL or Gravity.CENTER

                addView(TextView(_ctx).apply {
                    text = _data.getString("freeze_count")
                    setTextSize(TypedValue.COMPLEX_UNIT_DIP, content_fontsize)
                    setTextColor(_ctx.resources.getColor(R.color.theme01_textColorNormal))
                })
            })
            // 右边
            addView(LinearLayout(_ctx).apply {
                val _layout_params = LinearLayout.LayoutParams(0.dp, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                layoutParams = _layout_params
                gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT

                addView(TextView(_ctx).apply {
                    text = _data.getString("fee")
                    setTextSize(TypedValue.COMPLEX_UNIT_DIP, content_fontsize)
                    setTextColor(_ctx.resources.getColor(R.color.theme01_textColorNormal))
                })
            })
        }

        // 第四行 转入 转出 (操作)

        // 第三行 可用 冻结 平台手续费(值)
        val ly4 = LinearLayout(_ctx).apply {
            layoutParams = layout_params
            orientation = LinearLayout.HORIZONTAL

            // 转入
            addView(LinearLayout(_ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0.dp, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                gravity = Gravity.CENTER_VERTICAL or Gravity.CENTER

                addView(TextView(_ctx).apply {
                    text = "转入"
                    setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16.0f)
                    setTextColor(_ctx.resources.getColor(R.color.theme01_textColorHighlight))

                    setOnClickListener { onTransferClicked(1) }
                })
            })
            // 转出
            addView(LinearLayout(_ctx).apply {
                val _layout_params = LinearLayout.LayoutParams(0.dp, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                layoutParams = _layout_params
                gravity = Gravity.CENTER_VERTICAL or Gravity.CENTER

                addView(TextView(_ctx).apply {
                    text = "转出"
                    setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16.0f)
                    setTextColor(_ctx.resources.getColor(R.color.theme01_textColorHighlight))

                    setOnClickListener { onTransferClicked(2) }
                })
            })
        }

        layout_wrap.addView(ly1)
        layout_wrap.addView(ly2)
        layout_wrap.addView(ly3)
        layout_wrap.addView(ly4)

        addView(layout_wrap)
        return this
    }

    private fun onTransferClicked(type: Int){
        val _args = JSONObject().apply { put("type", type) }
        (_ctx as Activity).goTo(ActivityOtcMcAssetTransfer::class.java, args = _args)
    }
}