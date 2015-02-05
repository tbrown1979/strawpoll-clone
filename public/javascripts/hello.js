$(function() {
  function getIdFromUrl() {
    var url = window.location.pathname;
    var id = url.substring(url.lastIndexOf('/') + 1);
    return id;
  }

  $(".castVote").click(function( event ) {
    console.log("Handler called");
    //event.preventDefault();

    var index = $("input[name=option]:checked").val()
    var id = getIdFromUrl();
    var strData = JSON.stringify({"pollId": id, "index": parseInt(index)});
    console.log(strData);
    $.ajax({
      type: "POST",
      url: "/api/poll/vote",
      data: strData,
      success: function(data) {
        console.log(data);
        //window.location.href = "/" + id + "/r";
      },
      contentType: "application/json"
    })
  });

  $("button.right").click(function(event) {
    window.location.href = window.location.href + "/r";
  })

});
