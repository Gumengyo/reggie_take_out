var web_prefix = '/front'
var img_prefix = 'http://rvvy1q91l.hn-bkt.clouddn.com/reggie/'

function imgPath(path){
    return 'http://rvvy1q91l.hn-bkt.clouddn.com/reggie/' + path
}

//将url传参转换为数组
function parseUrl(url) {
    // 找到url中的第一个?号
    var parse = url.substring(url.indexOf("?") + 1),
        params = parse.split("&"),
        len = params.length,
        item = [],
        param = {};

    for (var i = 0; i < len; i++) {
        item = params[i].split("=");
        param[item[0]] = item[1];
    }

    return param;
}

