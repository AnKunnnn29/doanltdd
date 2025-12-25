package com.example.doan

import android.content.Context
import android.content.SharedPreferences
import com.example.doan.Models.Product
import com.example.doan.Utils.CartManager
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

/**
 * FIX Medium #11: Unit tests cho CartManager
 * 
 * Note: CartManager đã được đánh dấu deprecated (High #6)
 * Các tests này vẫn hữu ích để đảm bảo backward compatibility
 */
class CartManagerTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences
    
    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor
    
    private lateinit var cartManager: CartManager
    
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        
        `when`(mockContext.applicationContext).thenReturn(mockContext)
        `when`(mockContext.getSharedPreferences(anyString(), anyInt()))
            .thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        `when`(mockEditor.putLong(anyString(), anyLong())).thenReturn(mockEditor)
        `when`(mockSharedPreferences.getString(anyString(), isNull())).thenReturn(null)
        `when`(mockSharedPreferences.getLong(anyString(), anyLong())).thenReturn(System.currentTimeMillis())
        
        cartManager = CartManager.getInstance()
        cartManager.init(mockContext)
    }
    
    @Test
    fun `getCartItems returns empty list initially`() {
        val items = cartManager.getCartItems()
        assertTrue(items.isEmpty())
    }
    
    @Test
    fun `getItemCount returns 0 initially`() {
        assertEquals(0, cartManager.getItemCount())
    }
    
    @Test
    fun `getTotalPrice returns 0 initially`() {
        assertEquals(0.0, cartManager.getTotalPrice(), 0.01)
    }
    
    @Test
    fun `addToCart increases item count`() {
        val product = Product(
            id = 1,
            name = "Test Drink",
            description = "Test Description",
            price = 30000.0,
            imageUrl = "http://example.com/image.jpg"
        )
        
        cartManager.addToCart(product, 2, "M", 30000.0)
        
        assertEquals(1, cartManager.getItemCount())
    }
    
    @Test
    fun `addToCart calculates total price correctly`() {
        val product = Product(
            id = 1,
            name = "Test Drink",
            description = "Test Description",
            price = 30000.0,
            imageUrl = "http://example.com/image.jpg"
        )
        
        cartManager.addToCart(product, 2, "M", 30000.0)
        
        assertEquals(60000.0, cartManager.getTotalPrice(), 0.01)
    }
    
    @Test
    fun `clearCart removes all items`() {
        val product = Product(
            id = 1,
            name = "Test Drink",
            description = "Test Description",
            price = 30000.0,
            imageUrl = "http://example.com/image.jpg"
        )
        
        cartManager.addToCart(product, 1, "M", 30000.0)
        cartManager.clearCart()
        
        assertEquals(0, cartManager.getItemCount())
        assertEquals(0.0, cartManager.getTotalPrice(), 0.01)
    }
    
    @Test
    fun `updateQuantity changes item quantity`() {
        val product = Product(
            id = 1,
            name = "Test Drink",
            description = "Test Description",
            price = 30000.0,
            imageUrl = "http://example.com/image.jpg"
        )
        
        cartManager.addToCart(product, 1, "M", 30000.0)
        cartManager.updateQuantity(0, 3)
        
        val items = cartManager.getCartItems()
        assertEquals(3, items[0].quantity)
        assertEquals(90000.0, cartManager.getTotalPrice(), 0.01)
    }
    
    @Test
    fun `updateQuantity with 0 removes item`() {
        val product = Product(
            id = 1,
            name = "Test Drink",
            description = "Test Description",
            price = 30000.0,
            imageUrl = "http://example.com/image.jpg"
        )
        
        cartManager.addToCart(product, 1, "M", 30000.0)
        cartManager.updateQuantity(0, 0)
        
        assertEquals(0, cartManager.getItemCount())
    }
    
    @Test
    fun `removeItem removes specific item`() {
        val product1 = Product(
            id = 1,
            name = "Drink 1",
            description = "Description 1",
            price = 30000.0,
            imageUrl = "http://example.com/image1.jpg"
        )
        val product2 = Product(
            id = 2,
            name = "Drink 2",
            description = "Description 2",
            price = 40000.0,
            imageUrl = "http://example.com/image2.jpg"
        )
        
        cartManager.addToCart(product1, 1, "M", 30000.0)
        cartManager.addToCart(product2, 1, "L", 40000.0)
        cartManager.removeItem(0)
        
        assertEquals(1, cartManager.getItemCount())
        assertEquals("Drink 2", cartManager.getCartItems()[0].drinkName)
    }
    
    @Test
    fun `addToCart with same product and size increases quantity`() {
        val product = Product(
            id = 1,
            name = "Test Drink",
            description = "Test Description",
            price = 30000.0,
            imageUrl = "http://example.com/image.jpg"
        )
        
        cartManager.addToCart(product, 1, "M", 30000.0)
        cartManager.addToCart(product, 2, "M", 30000.0)
        
        // Should still be 1 item but with quantity 3
        assertEquals(1, cartManager.getItemCount())
        assertEquals(3, cartManager.getCartItems()[0].quantity)
        assertEquals(90000.0, cartManager.getTotalPrice(), 0.01)
    }
    
    @Test
    fun `addToCart with same product but different size creates new item`() {
        val product = Product(
            id = 1,
            name = "Test Drink",
            description = "Test Description",
            price = 30000.0,
            imageUrl = "http://example.com/image.jpg"
        )
        
        cartManager.addToCart(product, 1, "M", 30000.0)
        cartManager.addToCart(product, 1, "L", 35000.0)
        
        // Should be 2 different items
        assertEquals(2, cartManager.getItemCount())
    }
}
