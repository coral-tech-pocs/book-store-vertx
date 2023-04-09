package coral.bookstore.bookstore.entity

data class Author(var id : Long, var name : String){
  constructor() : this(0,"")
}
data class Book(var id : Long, var isbn : String, var title : String, var description : String, var price : Long){
  constructor() : this(0,"", "", "", 0)
}
data class Publisher(var id : Long, var name : String){
  constructor() : this(0,"")
}
