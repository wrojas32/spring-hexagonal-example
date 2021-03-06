package com.rdelgatte.hexagonal.api;

import static io.vavr.API.List;
import static io.vavr.API.None;
import static io.vavr.API.Option;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.rdelgatte.hexagonal.domain.Customer;
import com.rdelgatte.hexagonal.domain.Product;
import com.rdelgatte.hexagonal.spi.CustomerRepository;
import com.rdelgatte.hexagonal.spi.ProductRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

  private static final UUID ANY_PRODUCT_ID = randomUUID();
  private static final UUID ANY_OTHER_PRODUCT_ID = randomUUID();
  private static final String ANY_PRODUCT_CODE = "ANY_PRODUCT_CODE";
  private static final String ANY_OTHER_PRODUCT_CODE = "ANY_OTHER_PRODUCT_CODE";
  private static final String ANY_LABEL = "ANY_LABEL";
  private static final String ANY_OTHER_LABEL = "ANY_OTHER_LABEL";
  private static final Product ANY_PRODUCT = new Product(ANY_PRODUCT_ID, ANY_PRODUCT_CODE, ANY_LABEL,
      BigDecimal.valueOf(13.40));
  private static final Product ANY_OTHER_PRODUCT = new Product(ANY_OTHER_PRODUCT_ID, ANY_OTHER_PRODUCT_CODE,
      ANY_OTHER_LABEL, BigDecimal.valueOf(5.23));
  private static final UUID ANY_CUSTOMER_ID = randomUUID();
  private static final String ANY_NAME = "ANY_NAME";
  private static final Customer ANY_CUSTOMER = new Customer(ANY_CUSTOMER_ID, ANY_NAME, List());

  private CustomerServiceImpl cut;
  @Mock
  private CustomerRepository customerRepositoryMock;
  @Mock
  private ProductRepository productRepositoryMock;
  @Captor
  private ArgumentCaptor<Customer> customerCaptor;

  @BeforeEach
  void setUp() {
    cut = new CustomerServiceImpl(customerRepositoryMock, productRepositoryMock);
  }

  /**
   * {@link CustomerServiceImpl#signUp(String)}
   */
  @Test
  void customerAlreadyExists_throwsException() {
    when(customerRepositoryMock.findByLogin(ANY_NAME)).thenReturn(Option(ANY_CUSTOMER));

    IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
        () -> cut.signUp(ANY_NAME));
    assertThat(illegalArgumentException.getMessage()).isEqualTo("Customer already exists so you can't sign in");
    verifyNoMoreInteractions(customerRepositoryMock);
    verifyZeroInteractions(productRepositoryMock);
  }

  @Test
  void blankLogin_throwsException() {
    IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
        () -> cut.signUp(""));

    assertThat(illegalArgumentException.getMessage()).isEqualTo("Customer name should not be blank");
    verifyNoMoreInteractions(customerRepositoryMock);
    verifyZeroInteractions(productRepositoryMock);
  }

  @Test
  void validUnknownCustomer_signIn() {
    Customer expected = new Customer().withName(ANY_NAME);
    when(customerRepositoryMock.findByLogin(ANY_NAME)).thenReturn(None());
    when(customerRepositoryMock.save(any())).thenReturn(expected);

    Customer customer = cut.signUp(ANY_NAME);

    verify(customerRepositoryMock).save(customerCaptor.capture());
    assertThat(customerCaptor.getValue().getName()).isEqualTo(ANY_NAME);
    assertThat(customerCaptor.getValue().getCart()).isEqualTo(List());
    assertThat(customer.getName()).isEqualTo(ANY_NAME);
    verifyZeroInteractions(productRepositoryMock);
  }

  /**
   * {@link CustomerServiceImpl#addProductToCart(String, String)}
   */

  @Test
  void unknownCustomer_addProductToCart_throwsIllegalArgumentException() {
    when(customerRepositoryMock.findByLogin(ANY_NAME)).thenReturn(None());

    IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
        () -> cut.addProductToCart(ANY_NAME, ANY_PRODUCT_CODE));
    assertThat(illegalArgumentException.getMessage()).isEqualTo("The customer does not exist");
    verifyNoMoreInteractions(customerRepositoryMock);
    verifyZeroInteractions(productRepositoryMock);
  }

  @Test
  void unknownProduct_addProductToCart_throwsIllegalArgumentException() {
    when(customerRepositoryMock.findByLogin(ANY_NAME)).thenReturn(Option(ANY_CUSTOMER));
    when(productRepositoryMock.findProductByCode(ANY_PRODUCT_CODE)).thenReturn(None());

    IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
        () -> cut.addProductToCart(ANY_NAME, ANY_PRODUCT_CODE));
    assertThat(illegalArgumentException.getMessage()).isEqualTo("The product does not exist");
    verifyNoMoreInteractions(customerRepositoryMock);
    verifyNoMoreInteractions(productRepositoryMock);
  }

  @Test
  void existingProductAndCustomer_addProductToCart_returnsUpdatedCustomer() {
    Customer expected = ANY_CUSTOMER.withCart(List(ANY_PRODUCT));
    when(customerRepositoryMock.findByLogin(ANY_NAME)).thenReturn(Option(ANY_CUSTOMER));
    when(productRepositoryMock.findProductByCode(ANY_PRODUCT_CODE)).thenReturn(Option(ANY_PRODUCT));
    when(customerRepositoryMock.save(any())).thenReturn(expected);

    Customer customer = cut.addProductToCart(ANY_NAME, ANY_PRODUCT_CODE);
    verify(customerRepositoryMock).save(customerCaptor.capture());
    Customer savedCustomer = customerCaptor.getValue();
    assertThat(savedCustomer.getCart()).containsExactly(ANY_PRODUCT);
    assertThat(customer.getCart()).containsExactly(ANY_PRODUCT);
  }

  @Test
  void existingProductAndCustomerWithCart_addProductToCart_returnsUpdatedCustomer() {
    Customer existing = ANY_CUSTOMER.withCart(List(ANY_PRODUCT));
    Customer expected = existing.withCart(List(ANY_PRODUCT, ANY_OTHER_PRODUCT));
    when(customerRepositoryMock.findByLogin(ANY_NAME)).thenReturn(Option(existing));
    when(productRepositoryMock.findProductByCode(ANY_OTHER_PRODUCT_CODE)).thenReturn(Option(ANY_OTHER_PRODUCT));
    when(customerRepositoryMock.save(any())).thenReturn(expected);

    Customer customer = cut.addProductToCart(ANY_NAME, ANY_OTHER_PRODUCT_CODE);
    verify(customerRepositoryMock).save(customerCaptor.capture());
    Customer savedCustomer = customerCaptor.getValue();
    assertThat(savedCustomer.getCart()).containsExactly(ANY_PRODUCT, ANY_OTHER_PRODUCT);
    assertThat(customer.getCart()).containsExactly(ANY_PRODUCT, ANY_OTHER_PRODUCT);
  }

  /**
   * {@link CustomerServiceImpl#emptyCart(String)}
   */

  @Test
  void unknownCustomer_emptyCart_throwsIllegalArgumentException() {
    when(customerRepositoryMock.findByLogin(ANY_NAME)).thenReturn(None());

    IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
        () -> cut.emptyCart(ANY_NAME));
    assertThat(illegalArgumentException.getMessage()).isEqualTo("The customer does not exist");
    verifyNoMoreInteractions(customerRepositoryMock);
  }

  @Test
  void existingCustomer_emptyCart_returnsCustomerWithEmptiedCart() {
    Customer expected = ANY_CUSTOMER.withCart(List());
    when(customerRepositoryMock.findByLogin(ANY_NAME)).thenReturn(Option(ANY_CUSTOMER));
    when(customerRepositoryMock.save(any())).thenReturn(expected);

    Customer customer = cut.emptyCart(ANY_NAME);
    assertThat(customer).isEqualTo(expected);
    verify(customerRepositoryMock).save(expected);
  }

  /**
   * {@link CustomerServiceImpl#findCustomer(String)}
   */

  @Test
  void unknownCustomer_findCustomer_returnsNone() {
    when(customerRepositoryMock.findByLogin(ANY_NAME)).thenReturn(None());

    assertTrue(cut.findCustomer(ANY_NAME).isEmpty());
  }

  @Test
  void existingCustomer_findCustomer_returnsCustomer() {
    when(customerRepositoryMock.findByLogin(ANY_NAME)).thenReturn(Option(ANY_CUSTOMER));

    assertThat(cut.findCustomer(ANY_NAME)).isEqualTo(Option(ANY_CUSTOMER));
  }

  /**
   * {@link Customer#getCartTotal()}
   */

  @Test
  void emptyCart_getCartTotal_returnsZero() {
    assertThat(ANY_CUSTOMER.withCart(List()).getCartTotal()).isZero();
  }

  @Test
  void cartWithProducts_getCartTotal_returnsSumOfProductPrices() {
    assertThat(ANY_CUSTOMER.withCart(List(ANY_PRODUCT, ANY_OTHER_PRODUCT)).getCartTotal())
        .isEqualTo(BigDecimal.valueOf(18.63));
  }
}
