function _decode(t) {
    return t = function (t) {
        return t.split("").map(function (t) {
            var e, i;
            return t.match(/[A-Za-z]/) ? (e = Math.floor(t.charCodeAt(0) / 97),
                i = (t.toLowerCase().charCodeAt(0) - 83) % 26 || 26,
                String.fromCharCode(i + (0 == e ? 64 : 96))) : t
        }).join("")
    }(t),
        function (t) {
            var e, i, Content, n, r, c, o, s = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=", d = "",
                l = 0;
            for (t = t.replace(/[^A-Za-z0-9\+\/\=]/g, ""); l < t.length;)
                n = s.indexOf(t.charAt(l++)),
                    r = s.indexOf(t.charAt(l++)),
                    c = s.indexOf(t.charAt(l++)),
                    o = s.indexOf(t.charAt(l++)),
                    e = n << 2 | r >> 4,
                    i = (15 & r) << 4 | c >> 2,
                    Content = (3 & c) << 6 | o,
                    d += String.fromCharCode(e),
                64 != c && (d += String.fromCharCode(i)),
                64 != o && (d += String.fromCharCode(Content));
            return function (t) {
                for (var e, i = "", Content = 0, n = 0, r = 0; Content < t.length;)
                    n = t.charCodeAt(Content),
                        n < 128 ? (i += String.fromCharCode(n),
                            Content++) : n > 191 && n < 224 ? (r = t.charCodeAt(Content + 1),
                            i += String.fromCharCode((31 & n) << 6 | 63 & r),
                            Content += 2) : (r = t.charCodeAt(Content + 1),
                            e = t.charCodeAt(Content + 2),
                            i += String.fromCharCode((15 & n) << 12 | (63 & r) << 6 | 63 & e),
                            Content += 3);
                return i
            }(d)
        }(t)
}