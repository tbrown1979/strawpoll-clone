$(function() {
  $("#castVote").submit(function( event ) {
    console.log("Handler called");
    event.preventDefault();

    var index = $("input[name=option]:checked").val()
    var url = window.location.pathname;
    var id = url.substring(url.lastIndexOf('/') + 1);
    var strData = JSON.stringify({"pollId": id, "index": parseInt(index)});
    console.log(strData);
    $.ajax({
      type: "POST",
      url: "/api/poll/vote",
      data: strData,
      success: function(data) {
        window.location.href = "/" + id + "/r";
      },
      contentType: "application/json"
    })
  })
});

if (window.console) {
  console.log("Welcome to your Play application's JavaScript!");
}
