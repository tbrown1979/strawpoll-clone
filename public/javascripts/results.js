$(function() {
  function getIdFromUrl() {
    var url = window.location.pathname;
    function getId(url) {
      var split = url.split("/");
      return split[split.length-2];
    }
    console.log(getId(url));
    return getId(url);
  }

  var id = getIdFromUrl();
  var pollSocket = new WebSocket("ws://localhost:9000/ws/votes/" + id);//
  pollSocket.onmessage = function (event) {
    console.log(event.data);
    var data = JSON.parse(event.data);
    var tallies = $("div div div.pollStats");
    console.log(tallies);
    $("div div div.pollStats span.votes").map(function(i, v) {
      $(this).html(data[i]);
      return data[i];
    });
  }
});
