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
    console.log(options);
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

})
