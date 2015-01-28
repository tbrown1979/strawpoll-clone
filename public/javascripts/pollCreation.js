$(function() {
  $("#createPoll").submit(function( event ) {
    var options = $("div.optionDiv input")
      .map(function(i, e) {return $(e).val();});
    console.log(options);
    event.preventDefault();
  });
})
