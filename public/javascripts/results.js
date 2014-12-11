$(function() {
  var id = getIdFromUrl();
  var pollSocket = new WebSocket("ws://localhost:9000/ws/votes/" + id);//
  pollSocket.onmessage = function (event) {
    console.log(event.data);
  }
});
