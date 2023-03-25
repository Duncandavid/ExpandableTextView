package com.primary.expandabletextview

/**
 * Created by David at 2023/3/25
 */
object Utils {
    fun trim(cs: CharSequence): CharSequence {
        var len = cs.length
        var st = 0
        while (st < len && cs[st] <= ' ') {
            st++
        }
        while (st < len && cs[len - 1] <= ' ') {
            len--
        }
        return if (st > 0 || len < cs.length) cs.subSequence(st, len) else cs
    }
}