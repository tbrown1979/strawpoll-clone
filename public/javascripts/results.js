$(function() {
  function getIdFromUrl() {
    var url = window.location.pathname;
    var id = url.substring(url.lastIndexOf('/')-1, url.lastIndexOf('/'));
    console.log(id);
    return id;
  }

  var id = getIdFromUrl();
  var pollSocket = new WebSocket("ws://localhost:9000/ws/votes/" + id);//
  pollSocket.onmessage = function (event) {
    console.log(event.data);
  }
});
