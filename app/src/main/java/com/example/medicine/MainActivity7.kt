package com.example.medicine

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MainActivity7 : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var productKey: String

    private lateinit var ivProductImage: ImageView
    private lateinit var tvProductName: TextView
    private lateinit var tvProductCategory: TextView
    private lateinit var tvProductPrice: TextView
    private lateinit var tvProductStock: TextView
    private lateinit var tvProductDescription: TextView
    private lateinit var btnPlus: Button
    private lateinit var btnMinus: Button
    private lateinit var tvQuantity: TextView
    private lateinit var btnAddToCart: Button
    private lateinit var btnWishlist: ImageButton
    private lateinit var wishlistRef: DatabaseReference

    private var quantity = 1
    private var productPrice = 0.0
    private var productName = ""
    private var productImage: String? = null
    private var productStock = 0
    private var productCategory: String? = null
    private var productDescription: String? = null
    private var isWishlisted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main7)

        productKey = intent.getStringExtra("productId") ?: run {
            Toast.makeText(this, "Invalid product", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        database = FirebaseDatabase.getInstance().getReference("products")
        wishlistRef = FirebaseDatabase.getInstance().getReference("wishlist").child(uid)

        initViews()
        loadProduct()
        observeWishlist()

        btnPlus.setOnClickListener {
            if (quantity < productStock) {
                quantity++
                tvQuantity.text = quantity.toString()
                updatePrice()
            } else {
                Toast.makeText(this, "No more stock available", Toast.LENGTH_SHORT).show()
            }
        }

        btnMinus.setOnClickListener {
            if (quantity > 1) {
                quantity--
                tvQuantity.text = quantity.toString()
                updatePrice()
            }
        }

        btnAddToCart.setOnClickListener {
            database.child(productKey).child("stock")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val liveStock = snapshot.getValue(Int::class.java) ?: 0
                        when {
                            liveStock <= 0 -> Toast.makeText(
                                this@MainActivity7,
                                "Out of stock",
                                Toast.LENGTH_SHORT
                            ).show()

                            liveStock < quantity -> Toast.makeText(
                                this@MainActivity7,
                                "Only $liveStock unit(s) available",
                                Toast.LENGTH_SHORT
                            ).show()

                            else -> addToCart(uid)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@MainActivity7, error.message, Toast.LENGTH_SHORT).show()
                    }
                })
        }

        btnWishlist.setOnClickListener {
            toggleWishlist(uid)
        }
    }

    private fun initViews() {
        ivProductImage = findViewById(R.id.ivProductImage)
        tvProductName = findViewById(R.id.tvProductName)
        tvProductCategory = findViewById(R.id.tvProductCategory)
        tvProductPrice = findViewById(R.id.tvProductPrice)
        tvProductStock = findViewById(R.id.tvProductStock)
        tvProductDescription = findViewById(R.id.tvProductDescription)
        btnPlus = findViewById(R.id.btnPlus)
        btnMinus = findViewById(R.id.btnMinus)
        tvQuantity = findViewById(R.id.tvQuantity)
        btnAddToCart = findViewById(R.id.btnAddToCart)
        btnWishlist = findViewById(R.id.btnWishlist)
    }

    private fun loadProduct() {
        database.child(productKey)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val product = snapshot.getValue(Product::class.java)

                    if (product != null) {
                        productName = product.name ?: ""
                        productPrice = product.price ?: 0.0
                        productStock = product.stock ?: 0
                        productImage = product.image
                        productCategory = product.category
                        productDescription = product.description

                        tvProductName.text = productName
                        tvProductCategory.text = "Category: ${product.category}"
                        tvProductStock.text = "In Stock: $productStock"
                        tvProductDescription.text = product.description

                        if (productStock == 0) {
                            btnAddToCart.isEnabled = false
                            btnAddToCart.text = "Out of Stock"
                            tvProductStock.text = "Out of Stock"
                        }

                        updatePrice()

                        product.image?.let {
                            try {
                                val decoded = Base64.decode(it, Base64.DEFAULT)
                                val bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                                ivProductImage.setImageBitmap(bmp)
                            } catch (e: Exception) {
                                Log.e("PRODUCT_IMAGE", "Decode failed: ${e.message}")
                            }
                        }
                    } else {
                        Toast.makeText(this@MainActivity7, "Product not found", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@MainActivity7, error.message, Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updatePrice() {
        tvProductPrice.text = "₹%.2f".format(productPrice * quantity)
    }

    private fun addToCart(uid: String) {

        // ✅ SINGLE FIXED PATH — cart/{uid}/{cartId}
        val cartRef = FirebaseDatabase.getInstance()
            .getReference("cart")
            .child(uid)

        Log.d("CART_WRITE", "Writing to path: cart/$uid")

        // ✅ Check if product already exists in cart
        cartRef.orderByChild("productId").equalTo(productKey)
            .addListenerForSingleValueEvent(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    if (snapshot.exists()) {
                        // ✅ Update existing cart item
                        val existingSnap = snapshot.children.first()
                        val existingItem = existingSnap.getValue(CartItem::class.java)
                        val existingQty = existingItem?.quantity ?: 0
                        val newQty = existingQty + quantity
                        val newPrice = productPrice * newQty

                        existingSnap.ref.child("quantity").setValue(newQty)
                        existingSnap.ref.child("price").setValue(newPrice)

                        Log.d("CART_WRITE", "Updated existing item qty to $newQty")

                    } else {
                        // ✅ Create new cart item
                        val cartId = cartRef.push().key ?: return

                        val cartItem = CartItem(
                            productId = productKey,
                            name = productName,
                            price = productPrice * quantity,
                            quantity = quantity,
                            image = productImage,
                            unitPrice = productPrice,
                            uid = uid
                        )

                        cartRef.child(cartId).setValue(cartItem)
                            .addOnSuccessListener {
                                Log.d("CART_WRITE", "Item saved at cart/$uid/$cartId")
                            }
                            .addOnFailureListener {
                                Log.e("CART_WRITE", "Failed: ${it.message}")
                                Toast.makeText(this@MainActivity7, "Failed to add to cart", Toast.LENGTH_SHORT).show()
                            }
                    }

                    // ✅ Deduct stock
                    val newStock = productStock - quantity
                    database.child(productKey).child("stock").setValue(newStock)

                    Toast.makeText(this@MainActivity7, "Added to cart ✓", Toast.LENGTH_SHORT).show()
                    finish()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@MainActivity7, error.message, Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun observeWishlist() {
        wishlistRef.child(productKey).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                isWishlisted = snapshot.exists()
                updateWishlistIcon()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun toggleWishlist(uid: String) {
        if (isWishlisted) {
            wishlistRef.child(productKey).removeValue()
                .addOnSuccessListener {
                    Toast.makeText(this, "Removed from wishlist", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to update wishlist", Toast.LENGTH_SHORT).show()
                }
            return
        }

        val item = WishlistItem(
            productId = productKey,
            name = productName,
            price = productPrice,
            image = productImage,
            category = productCategory,
            stock = productStock,
            description = productDescription,
            uid = uid
        )

        wishlistRef.child(productKey).setValue(item)
            .addOnSuccessListener {
                Toast.makeText(this, "Added to wishlist", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update wishlist", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateWishlistIcon() {
        btnWishlist.setImageResource(
            if (isWishlisted) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
        )
    }
}