function callServerStopSparkFunction() {
    $.ajax({
        type: 'GET',
        url: 'http://127.0.0.1:8081/stop_spark'
    });
}