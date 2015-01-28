$(function() {

  function getOptions(asVal) {
    var options = $("div.optionDiv input")
    if (asVal) {
      return options.map(function(i, e) {return $(e).val();});
    }
    return options;
  }

  function lastOption() {
    var options = getOptions(false);
    return options[options.length-1]
  }

  $("#createPoll").submit(function( event ) {
    var options = getOptions(true);
    event.preventDefault();
  });

  function addAnotherTextfield() {
    var options = getOptions(false);
    $(lastOption()).unbind("click");
    $(lastOption()).after(
      "<br><input type='text' name='option" + options.length + "'>"
    );
    $(lastOption()).click(addAnotherTextfield);
  }

  $(lastOption()).click(addAnotherTextfield);

  $("#createPoll").submit(function(event) {
    event.preventDefault();
    console.log("Handler working!");
    var title = $(".title").val();
    var options = _.filter(getOptions(true), function(e) { return e; });
    var newPoll = {title: title, options: options};
    console.log(newPoll);
    $.ajax({
      type: "POST",
      url: "/api/poll/new",
      data: JSON.stringify(newPoll),
      success: function(data) {
        console.log(data);
      },
      contentType: "application/json"
    })
  })
})
