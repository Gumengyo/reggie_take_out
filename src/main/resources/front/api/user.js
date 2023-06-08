//修改用户信息
function  updateUserInfoApi(data){
    return $axios({
        'url': '/user',
        'method': 'put',
        data
    })
}

//查询当前用户信息
function orderListApi() {
    return $axios({
        'url': '/user',
        'method': 'get',
    })
}


