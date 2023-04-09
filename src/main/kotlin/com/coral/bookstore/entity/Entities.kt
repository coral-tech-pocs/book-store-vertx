package coral.bookstore.bookstore.entity

data class Author(var id : Long, var name : String)
data class Book(var id : Long, var isbn : String, var title : String, var description : String, var price : Long)
data class Publisher(var id : Long, var name : String)
